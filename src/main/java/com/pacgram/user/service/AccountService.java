package com.pacgram.user.service;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.auth.repository.PasswordResetTokenRepository;
import com.pacgram.auth.repository.UserAccountRepository;
import com.pacgram.common.exception.ApiException;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.common.storage.ImageStorageService;
import com.pacgram.messaging.repository.ConversationRepository;
import com.pacgram.messaging.repository.DirectMessageRepository;
import com.pacgram.moderation.repository.UserReportRepository;
import com.pacgram.notification.repository.NotificationRepository;
import com.pacgram.post.entity.Post;
import com.pacgram.post.repository.CommentRepository;
import com.pacgram.post.repository.PostLikeRepository;
import com.pacgram.post.repository.PostRepository;
import com.pacgram.post.repository.SavedPostRepository;
import com.pacgram.user.dto.DeleteAccountRequest;
import com.pacgram.user.entity.UserFollow;
import com.pacgram.user.repository.UserFollowRepository;
import com.pacgram.user.repository.UserBlockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserFollowRepository followRepository;
    private final UserBlockRepository blockRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final SavedPostRepository savedPostRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final UserReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorageService imageStorageService;

    public AccountService(
            UserAccountRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            UserFollowRepository followRepository,
            UserBlockRepository blockRepository,
            PostRepository postRepository,
            PostLikeRepository likeRepository,
            SavedPostRepository savedPostRepository,
            CommentRepository commentRepository,
            NotificationRepository notificationRepository,
            ConversationRepository conversationRepository,
            DirectMessageRepository messageRepository,
            UserReportRepository reportRepository,
            PasswordEncoder passwordEncoder,
            ImageStorageService imageStorageService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.savedPostRepository = savedPostRepository;
        this.commentRepository = commentRepository;
        this.notificationRepository = notificationRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public void deleteAccount(AccountPrincipal principal, DeleteAccountRequest request) {
        UserAccount user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD",
                    "Password confirmation did not match");
        }

        List<String> imageUrls = new ArrayList<>();
        if (user.getAvatarUrl() != null) imageUrls.add(user.getAvatarUrl());
        if (user.getCoverUrl() != null) imageUrls.add(user.getCoverUrl());
        List<Post> posts = postRepository.findAllByAuthorId(user.getId());
        posts.stream().map(Post::getImageUrl).filter(url -> url != null).forEach(imageUrls::add);

        List<Long> conversationIds = conversationRepository.findIdsForParticipant(user.getId());
        if (!conversationIds.isEmpty()) {
            messageRepository.deleteByConversationIdIn(conversationIds);
            conversationRepository.deleteByIdIn(conversationIds);
        }
        notificationRepository.deleteByRecipientIdOrActorId(user.getId(), user.getId());
        commentRepository.deleteByAuthorId(user.getId());
        likeRepository.deleteByUserId(user.getId());
        savedPostRepository.deleteByUserId(user.getId());
        if (!posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(Post::getId).toList();
            notificationRepository.deleteByPostIds(postIds);
            commentRepository.deleteByPostIds(postIds);
            likeRepository.deleteByPostIds(postIds);
            savedPostRepository.deleteByPostIds(postIds);
            postRepository.deleteAllByIdInBatch(postIds);
        }
        followRepository.deleteByFollowerIdOrFollowedId(user.getId());
        blockRepository.deleteByUserId(user.getId());
        reportRepository.deleteByUserId(user.getId());
        resetTokenRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String imageUrl : imageUrls) {
                    try {
                        imageStorageService.deleteImage(imageUrl);
                    } catch (RuntimeException exception) {
                        log.warn("Unable to remove an account image after deletion");
                    }
                }
            }
        });
    }
}
