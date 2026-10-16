package com.pacgram.notification.service;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.auth.repository.UserAccountRepository;
import com.pacgram.common.event.NotificationType;
import com.pacgram.common.event.SocialActivityEvent;
import com.pacgram.common.exception.ApiException;
import com.pacgram.common.pagination.Pageables;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.notification.dto.NotificationDto;
import com.pacgram.notification.entity.Notification;
import com.pacgram.notification.repository.NotificationRepository;
import com.pacgram.post.entity.Post;
import com.pacgram.post.repository.PostRepository;
import com.pacgram.user.mapper.UserMapper;
import com.pacgram.user.repository.UserFollowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userRepository;
    private final PostRepository postRepository;
    private final UserFollowRepository followRepository;
    private final UserMapper userMapper = new UserMapper();

    public NotificationService(
            NotificationRepository notificationRepository,
            UserAccountRepository userRepository,
            PostRepository postRepository,
            UserFollowRepository followRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.followRepository = followRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleSocialActivity(SocialActivityEvent event) {
        UserAccount recipient = userRepository.findById(event.recipientId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Notification recipient was not found"));
        if (!notificationsEnabled(recipient, event.type())) {
            return;
        }
        UserAccount actor = userRepository.findById(event.actorId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Notification actor was not found"));
        Post post = event.postId() == null ? null : postRepository.findById(event.postId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Notification post was not found"));
        String preview = event.preview() == null ? null
                : event.preview().substring(0, Math.min(event.preview().length(), 500));
        notificationRepository.save(new Notification(recipient, actor, post, event.type(), preview));
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> list(AccountPrincipal principal, Pageable pageable) {
        Page<Notification> notifications =
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                        principal.userId(), Pageables.withoutSort(pageable));
        List<Long> actorIds = notifications.getContent().stream()
                .map(notification -> notification.getActor().getId()).distinct().toList();
        Set<Long> followedIds = actorIds.isEmpty() ? Set.of()
                : Set.copyOf(followRepository.findFollowedUserIds(principal.userId(), actorIds));
        return notifications.map(notification -> new NotificationDto(
                notification.getId(),
                notification.getType(),
                userMapper.toSummary(notification.getActor(),
                        followedIds.contains(notification.getActor().getId())),
                notification.getPost() == null ? null : notification.getPost().getId(),
                notification.getPreview(),
                notification.isRead(),
                notification.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public long unreadCount(AccountPrincipal principal) {
        return notificationRepository.countByRecipientIdAndReadFalse(principal.userId());
    }

    @Transactional
    public void markRead(Long notificationId, AccountPrincipal principal) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification was not found"));
        if (!notification.getRecipient().getId().equals(principal.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "You may only update your own notifications");
        }
        notification.markRead();
    }

    @Transactional
    public void markAllRead(AccountPrincipal principal) {
        notificationRepository.markAllRead(principal.userId());
    }

    private boolean notificationsEnabled(UserAccount user, NotificationType type) {
        return switch (type) {
            case LIKE -> user.isNotifyLikes();
            case COMMENT -> user.isNotifyComments();
            case FOLLOW -> user.isNotifyFollows();
            case MESSAGE -> user.isNotifyMessages();
        };
    }
}
