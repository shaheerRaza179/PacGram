package com.pacgram.user.service;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.auth.repository.UserAccountRepository;
import com.pacgram.common.event.NotificationType;
import com.pacgram.common.event.SocialActivityEvent;
import com.pacgram.common.exception.ApiException;
import com.pacgram.common.pagination.Pageables;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.user.dto.UpdateProfileRequest;
import com.pacgram.user.dto.AccountSettingsDto;
import com.pacgram.user.dto.AccountSettingsRequest;
import com.pacgram.user.dto.UserProfileDto;
import com.pacgram.user.dto.UserSummaryDto;
import com.pacgram.user.entity.UserFollow;
import com.pacgram.user.mapper.UserMapper;
import com.pacgram.user.repository.UserFollowRepository;
import com.pacgram.user.repository.UserBlockRepository;
import com.pacgram.user.repository.UserSearchRepository;
import com.pacgram.post.service.PostService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.time.Instant;

@Service
public class UserProfileService {
    private final UserAccountRepository userRepository;
    private final UserSearchRepository searchRepository;
    private final UserFollowRepository followRepository;
    private final UserBlockRepository blockRepository;
    private final PostService postService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper = new UserMapper();

    public UserProfileService(
            UserAccountRepository userRepository,
            UserSearchRepository searchRepository,
            UserFollowRepository followRepository,
            UserBlockRepository blockRepository,
            PostService postService,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.searchRepository = searchRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.postService = postService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username, AccountPrincipal viewer) {
        UserAccount user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> notFound("User was not found"));
        ensureNotBlocked(viewer.userId(), user.getId());
        return mapProfile(user, viewer.userId());
    }

    @Transactional(readOnly = true)
    public UserProfileDto getOwnProfile(AccountPrincipal principal) {
        return mapProfile(findUser(principal.userId()), principal.userId());
    }

    @Transactional
    public void recordPresence(AccountPrincipal principal) {
        UserAccount user = findUser(principal.userId());
        user.recordPresence(Instant.now());
    }

    @Transactional
    public UserProfileDto updateProfile(AccountPrincipal principal, UpdateProfileRequest request) {
        UserAccount user = findUser(principal.userId());
        user.updateProfile(request.name().trim(), request.bio(), user.getAvatarUrl(), user.getCoverUrl());
        return mapProfile(user, principal.userId());
    }

    @Transactional(readOnly = true)
    public AccountSettingsDto settings(AccountPrincipal principal) {
        UserAccount user = findUser(principal.userId());
        return new AccountSettingsDto(user.isPrivateAccount(), user.isActivityStatus(),
                user.isNotifyLikes(), user.isNotifyComments(),
                user.isNotifyFollows(), user.isNotifyMessages());
    }

    @Transactional
    public AccountSettingsDto updateSettings(AccountPrincipal principal, AccountSettingsRequest request) {
        UserAccount user = findUser(principal.userId());
        user.updateSettings(request.privateAccount(), request.activityStatus(),
                request.notifyLikes(), request.notifyComments(),
                request.notifyFollows(), request.notifyMessages());
        return settings(principal);
    }

    @Transactional
    public UserProfileDto updateImage(AccountPrincipal principal, String imageType, String imageUrl) {
        UserAccount user = findUser(principal.userId());
        String avatarUrl = "avatar".equals(imageType) ? imageUrl : user.getAvatarUrl();
        String coverUrl = "cover".equals(imageType) ? imageUrl : user.getCoverUrl();
        user.updateProfile(user.getName(), user.getBio(), avatarUrl, coverUrl);
        return mapProfile(user, principal.userId());
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> search(String query, AccountPrincipal viewer, Pageable pageable) {
        return mapSummaries(searchRepository.searchUsers(
                viewer.userId(), query.trim(), Pageables.withoutSort(pageable)), viewer.userId());
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> suggestions(AccountPrincipal viewer, Pageable pageable) {
        return mapSummaries(followRepository.findSuggestions(
                viewer.userId(), Pageables.withoutSort(pageable)), viewer.userId());
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> followers(String username, AccountPrincipal viewer, Pageable pageable) {
        UserAccount user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> notFound("User was not found"));
        return mapSummaries(followRepository.findFollowers(
                user.getId(), viewer.userId(), Pageables.withoutSort(pageable)), viewer.userId());
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> followers(Long userId, AccountPrincipal viewer, Pageable pageable) {
        findUser(userId);
        return mapSummaries(followRepository.findFollowers(
                userId, viewer.userId(), Pageables.withoutSort(pageable)), viewer.userId());
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> following(String username, AccountPrincipal viewer, Pageable pageable) {
        UserAccount user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> notFound("User was not found"));
        return mapSummaries(followRepository.findFollowing(
                user.getId(), viewer.userId(), Pageables.withoutSort(pageable)), viewer.userId());
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> following(Long userId, AccountPrincipal viewer, Pageable pageable) {
        findUser(userId);
        return mapSummaries(followRepository.findFollowing(
                userId, viewer.userId(), Pageables.withoutSort(pageable)), viewer.userId());
    }

    @Transactional
    public void follow(AccountPrincipal principal, Long targetId) {
        if (principal.userId().equals(targetId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FOLLOW", "You cannot follow yourself");
        }
        UserAccount follower = findUser(principal.userId());
        UserAccount followed = findUser(targetId);
        ensureNotBlocked(follower.getId(), followed.getId());
        if (!followRepository.existsByFollowerIdAndFollowedId(follower.getId(), followed.getId())) {
            followRepository.save(new UserFollow(follower, followed));
            eventPublisher.publishEvent(new SocialActivityEvent(
                    follower.getId(), followed.getId(), NotificationType.FOLLOW, null, null));
        }
    }

    @Transactional
    public void unfollow(AccountPrincipal principal, Long targetId) {
        if (!userRepository.existsById(targetId)) {
            throw notFound("User was not found");
        }
        followRepository.deleteByFollowerIdAndFollowedId(principal.userId(), targetId);
    }

    @Transactional
    public void block(AccountPrincipal principal, Long targetId) {
        if (principal.userId().equals(targetId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BLOCK", "You cannot block your own account");
        }
        UserAccount blocker = findUser(principal.userId());
        UserAccount blocked = findUser(targetId);
        if (!blockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked.getId())) {
            blockRepository.save(new com.pacgram.user.entity.UserBlock(blocker, blocked));
        }
        followRepository.deleteByFollowerIdAndFollowedId(blocker.getId(), blocked.getId());
        followRepository.deleteByFollowerIdAndFollowedId(blocked.getId(), blocker.getId());
    }

    @Transactional
    public void unblock(AccountPrincipal principal, Long targetId) {
        blockRepository.deleteByBlockerIdAndBlockedId(principal.userId(), targetId);
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> blockedUsers(AccountPrincipal principal, Pageable pageable) {
        return mapSummaries(blockRepository.findBlockedUsers(
                principal.userId(), Pageables.withoutSort(pageable)), principal.userId());
    }

    @Transactional(readOnly = true)
    public UserAccount findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> notFound("User was not found"));
    }

    private UserProfileDto mapProfile(UserAccount user, Long viewerId) {
        return userMapper.toProfile(user, user.getId().equals(viewerId),
                followRepository.existsByFollowerIdAndFollowedId(viewerId, user.getId()),
                followRepository.countByFollowedId(user.getId()),
                followRepository.countByFollowerId(user.getId()),
                postService.postCount(user.getId()));
    }

    private void ensureNotBlocked(Long firstId, Long secondId) {
        if (!firstId.equals(secondId) && blockRepository.existsBlockBetween(firstId, secondId)) {
            throw notFound("User was not found");
        }
    }

    private Page<UserSummaryDto> mapSummaries(Page<UserAccount> users, Long viewerId) {
        List<Long> ids = users.getContent().stream().map(UserAccount::getId).toList();
        Set<Long> followedIds = ids.isEmpty() ? Set.of()
                : Set.copyOf(followRepository.findFollowedUserIds(viewerId, ids));
        List<UserSummaryDto> content = users.getContent().stream()
                .map(user -> userMapper.toSummary(user, followedIds.contains(user.getId())))
                .toList();
        return new PageImpl<>(content, users.getPageable(), users.getTotalElements());
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
