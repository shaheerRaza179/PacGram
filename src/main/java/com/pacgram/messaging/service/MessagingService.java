package com.pacgram.messaging.service;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.auth.repository.UserAccountRepository;
import com.pacgram.common.event.NotificationType;
import com.pacgram.common.event.SocialActivityEvent;
import com.pacgram.common.exception.ApiException;
import com.pacgram.common.pagination.Pageables;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.messaging.dto.ConversationDto;
import com.pacgram.messaging.dto.MessageDto;
import com.pacgram.messaging.dto.TypingEvent;
import com.pacgram.messaging.event.MessageCreatedEvent;
import com.pacgram.messaging.entity.Conversation;
import com.pacgram.messaging.entity.DirectMessage;
import com.pacgram.messaging.repository.ConversationRepository;
import com.pacgram.messaging.repository.DirectMessageRepository;
import com.pacgram.user.dto.UserSummaryDto;
import com.pacgram.user.mapper.UserMapper;
import com.pacgram.user.repository.UserFollowRepository;
import com.pacgram.user.repository.UserBlockRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessagingService {
    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final UserAccountRepository userRepository;
    private final UserFollowRepository followRepository;
    private final UserBlockRepository blockRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserMapper userMapper = new UserMapper();

    public MessagingService(
            ConversationRepository conversationRepository,
            DirectMessageRepository messageRepository,
            UserAccountRepository userRepository,
            UserFollowRepository followRepository,
            UserBlockRepository blockRepository,
            ApplicationEventPublisher eventPublisher,
            SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public Page<ConversationDto> listConversations(AccountPrincipal principal, Pageable pageable) {
        Page<Conversation> conversations = conversationRepository.findForUser(
                principal.userId(), Pageables.withoutSort(pageable));
        List<Long> ids = conversations.getContent().stream().map(Conversation::getId).toList();
        List<DirectMessage> lastMessages = ids.isEmpty() ? List.of() : messageRepository.findLastMessages(ids);
        Map<Long, DirectMessage> lastMessageByConversation = lastMessages.stream()
                .collect(Collectors.toMap(message -> message.getConversation().getId(), message -> message));
        Map<Long, Long> unreadByConversation = ids.isEmpty() ? Map.of()
                : messageRepository.countUnreadByConversation(ids, principal.userId()).stream()
                    .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        List<Long> otherIds = conversations.getContent().stream()
                .map(conversation -> conversation.otherParticipant(principal.userId()).getId())
                .distinct().toList();
        var followedIds = otherIds.isEmpty() ? java.util.Set.<Long>of()
                : java.util.Set.copyOf(followRepository.findFollowedUserIds(principal.userId(), otherIds));
        List<ConversationDto> content = conversations.getContent().stream()
                .map(conversation -> {
                    UserAccount participant = conversation.otherParticipant(principal.userId());
                    DirectMessage last = lastMessageByConversation.get(conversation.getId());
                    UserSummaryDto user = userMapper.toSummary(participant, followedIds.contains(participant.getId()));
                    return new ConversationDto(conversation.getId(), user,
                            last == null ? null : toDto(last),
                            unreadByConversation.getOrDefault(conversation.getId(), 0L),
                            conversation.getLastMessageAt());
                })
                .toList();
        return new PageImpl<>(content, conversations.getPageable(), conversations.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<MessageDto> listMessages(Long conversationId, AccountPrincipal principal, Pageable pageable) {
        Conversation conversation = findConversation(conversationId);
        ensureParticipant(conversation, principal.userId());
        ensureNotBlocked(principal.userId(), conversation.otherParticipant(principal.userId()).getId());
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversationId, Pageables.withoutSort(pageable)).map(this::toDto);
    }

    @Transactional
    public ConversationDto startConversation(Long otherUserId, AccountPrincipal principal) {
        if (principal.userId().equals(otherUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CONVERSATION",
                    "You cannot start a conversation with yourself");
        }
        UserAccount first = findUser(Math.min(principal.userId(), otherUserId));
        UserAccount second = findUser(Math.max(principal.userId(), otherUserId));
        ensureNotBlocked(principal.userId(), otherUserId);
        Conversation conversation = conversationRepository
                .findByUserOneIdAndUserTwoId(first.getId(), second.getId())
                .orElseGet(() -> conversationRepository.save(new Conversation(first, second)));
        UserAccount participant = conversation.otherParticipant(principal.userId());
        boolean followed = followRepository.existsByFollowerIdAndFollowedId(principal.userId(), participant.getId());
        return new ConversationDto(conversation.getId(), userMapper.toSummary(participant, followed),
                null, 0, conversation.getLastMessageAt());
    }

    @Transactional
    public MessageDto sendMessage(
            Long conversationId, AccountPrincipal principal, String text) {
        Conversation conversation = findConversation(conversationId);
        ensureParticipant(conversation, principal.userId());
        ensureNotBlocked(principal.userId(), conversation.otherParticipant(principal.userId()).getId());
        UserAccount sender = findUser(principal.userId());
        String normalizedText = text.trim();
        DirectMessage message = messageRepository.save(new DirectMessage(conversation, sender, normalizedText));
        conversation.markActive(message.getCreatedAt());
        Long recipientId = conversation.otherParticipant(principal.userId()).getId();
        eventPublisher.publishEvent(new SocialActivityEvent(
                sender.getId(), recipientId, NotificationType.MESSAGE, null, normalizedText));
        MessageDto dto = toDto(message);
        eventPublisher.publishEvent(new MessageCreatedEvent(conversationId, dto));
        return dto;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void broadcastMessage(MessageCreatedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + event.conversationId(), event.message());
    }

    @Transactional
    public void markRead(Long conversationId, AccountPrincipal principal) {
        Conversation conversation = findConversation(conversationId);
        ensureParticipant(conversation, principal.userId());
        ensureNotBlocked(principal.userId(), conversation.otherParticipant(principal.userId()).getId());
        messageRepository.markIncomingRead(conversationId, principal.userId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public void publishTyping(Long conversationId, AccountPrincipal principal, boolean typing) {
        Conversation conversation = findConversation(conversationId);
        ensureParticipant(conversation, principal.userId());
        UserAccount user = findUser(principal.userId());
        UserAccount recipient = conversation.otherParticipant(principal.userId());
        ensureNotBlocked(principal.userId(), recipient.getId());
        if (user.isActivityStatus()) {
            messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/typing",
                    new TypingEvent(conversationId, user.getId(), user.getUsername(), typing));
        }
    }

    private MessageDto toDto(DirectMessage message) {
        return new MessageDto(message.getId(), message.getSender().getId(),
                message.getSender().getUsername(), message.getText(),
                message.getCreatedAt(), message.isRead());
    }

    private Conversation findConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation was not found"));
    }

    private UserAccount findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
    }

    private void ensureParticipant(Conversation conversation, Long userId) {
        if (!conversation.hasParticipant(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "You are not a participant in this conversation");
        }
    }

    private void ensureNotBlocked(Long firstId, Long secondId) {
        if (blockRepository.existsBlockBetween(firstId, secondId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_BLOCKED",
                    "This conversation is unavailable because one account has blocked the other");
        }
    }
}
