package com.pacgram.post.service;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.auth.repository.UserAccountRepository;
import com.pacgram.common.event.NotificationType;
import com.pacgram.common.event.SocialActivityEvent;
import com.pacgram.common.exception.ApiException;
import com.pacgram.common.pagination.Pageables;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.common.storage.ImageStorageService;
import com.pacgram.post.dto.AddCommentRequest;
import com.pacgram.post.dto.CommentDto;
import com.pacgram.post.dto.CreatePostRequest;
import com.pacgram.post.dto.PostDto;
import com.pacgram.post.entity.Comment;
import com.pacgram.post.entity.Post;
import com.pacgram.post.entity.PostLike;
import com.pacgram.post.entity.SavedPost;
import com.pacgram.post.mapper.PostMapper;
import com.pacgram.post.repository.CommentRepository;
import com.pacgram.post.repository.PostLikeRepository;
import com.pacgram.post.repository.PostRepository;
import com.pacgram.post.repository.SavedPostRepository;
import com.pacgram.user.repository.UserFollowRepository;
import com.pacgram.user.repository.UserBlockRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final SavedPostRepository savedPostRepository;
    private final CommentRepository commentRepository;
    private final UserAccountRepository userRepository;
    private final UserFollowRepository followRepository;
    private final UserBlockRepository blockRepository;
    private final ImageStorageService imageStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final PostMapper postMapper = new PostMapper();

    public PostService(
            PostRepository postRepository,
            PostLikeRepository likeRepository,
            SavedPostRepository savedPostRepository,
            CommentRepository commentRepository,
            UserAccountRepository userRepository,
            UserFollowRepository followRepository,
            UserBlockRepository blockRepository,
            ImageStorageService imageStorageService,
            ApplicationEventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.savedPostRepository = savedPostRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.imageStorageService = imageStorageService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PostDto create(AccountPrincipal principal, CreatePostRequest request) {
        UserAccount author = findUser(principal.userId());
        String imageUrl = request.image() == null || request.image().isEmpty()
                ? null : imageStorageService.storeImage(request.image());
        Post post = postRepository.save(new Post(
                author, blankToNull(request.caption()), imageUrl));
        return toDto(post, principal);
    }

    @Transactional(readOnly = true)
    public PostDto get(Long postId, AccountPrincipal viewer) {
        Post post = findPost(postId);
        ensureVisible(post, viewer.userId());
        return toDto(post, viewer);
    }

    @Transactional(readOnly = true)
    public Page<PostDto> feed(AccountPrincipal viewer, Pageable pageable) {
        return mapPage(postRepository.findFeed(viewer.userId(), Pageables.withoutSort(pageable)), viewer);
    }

    @Transactional(readOnly = true)
    public Page<PostDto> explore(AccountPrincipal viewer, Pageable pageable) {
        return mapPage(postRepository.findExplore(viewer.userId(), Pageables.withoutSort(pageable)), viewer);
    }

    @Transactional(readOnly = true)
    public Page<PostDto> byUser(Long userId, AccountPrincipal viewer, Pageable pageable) {
        UserAccount author = findUser(userId);
        ensureVisible(author, viewer.userId());
        return mapPage(postRepository.findByAuthor(userId, Pageables.withoutSort(pageable)), viewer);
    }

    @Transactional(readOnly = true)
    public Page<PostDto> saved(AccountPrincipal viewer, Pageable pageable) {
        Page<SavedPost> savedPosts = savedPostRepository.findVisibleByUser(
                viewer.userId(), Pageables.withoutSort(pageable));
        List<Post> posts = savedPosts.getContent().stream()
                .map(SavedPost::getPost)
                .toList();
        return mapPage(new PageImpl<>(posts, savedPosts.getPageable(),
                savedPosts.getTotalElements()), viewer);
    }

    @Transactional
    public void delete(Long postId, AccountPrincipal principal) {
        Post post = findPost(postId);
        if (!post.getAuthor().getId().equals(principal.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You may only delete your own posts");
        }
        commentRepository.deleteByPostId(postId);
        likeRepository.deleteByPostId(postId);
        savedPostRepository.deleteByPostId(postId);
        postRepository.delete(post);
        imageStorageService.deleteImage(post.getImageUrl());
    }

    @Transactional
    public void like(Long postId, AccountPrincipal principal) {
        Post post = findPost(postId);
        ensureVisible(post, principal.userId());
        if (!likeRepository.existsByUserIdAndPostId(principal.userId(), postId)) {
            likeRepository.save(new PostLike(findUser(principal.userId()), post));
            publishActivity(principal.userId(), post.getAuthor().getId(),
                    NotificationType.LIKE, post.getId(), null);
        }
    }

    @Transactional
    public void unlike(Long postId, AccountPrincipal principal) {
        Post post = findPost(postId);
        ensureVisible(post, principal.userId());
        likeRepository.deleteByUserIdAndPostId(principal.userId(), postId);
    }

    @Transactional
    public void save(Long postId, AccountPrincipal principal) {
        Post post = findPost(postId);
        ensureVisible(post, principal.userId());
        if (!savedPostRepository.existsByUserIdAndPostId(principal.userId(), postId)) {
            savedPostRepository.save(new SavedPost(findUser(principal.userId()), post));
        }
    }

    @Transactional
    public void unsave(Long postId, AccountPrincipal principal) {
        savedPostRepository.deleteByUserIdAndPostId(principal.userId(), postId);
    }

    @Transactional
    public CommentDto addComment(Long postId, AccountPrincipal principal, AddCommentRequest request) {
        Post post = findPost(postId);
        ensureVisible(post, principal.userId());
        Comment comment = commentRepository.save(new Comment(
                findUser(principal.userId()), post, request.text().trim()));
        publishActivity(principal.userId(), post.getAuthor().getId(),
                NotificationType.COMMENT, post.getId(), request.text().trim());
        return postMapper.toDto(comment,
                followRepository.existsByFollowerIdAndFollowedId(principal.userId(),
                        comment.getAuthor().getId()));
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> comments(Long postId, AccountPrincipal principal, Pageable pageable) {
        Post post = findPost(postId);
        ensureVisible(post, principal.userId());
        Page<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(
                postId, Pageables.withoutSort(pageable));
        List<Long> authorIds = comments.getContent().stream()
                .map(comment -> comment.getAuthor().getId()).distinct().toList();
        var followedIds = authorIds.isEmpty() ? java.util.Set.<Long>of()
                : java.util.Set.copyOf(followRepository.findFollowedUserIds(principal.userId(), authorIds));
        List<CommentDto> content = comments.getContent().stream()
                .map(comment -> postMapper.toDto(comment, followedIds.contains(comment.getAuthor().getId())))
                .toList();
        return new PageImpl<>(content, pageable, comments.getTotalElements());
    }

    @Transactional(readOnly = true)
    public long postCount(Long userId) {
        return postRepository.countByAuthorId(userId);
    }

    private Page<PostDto> mapPage(Page<Post> posts, AccountPrincipal viewer) {
        if (posts.isEmpty()) {
            return new PageImpl<>(List.of(), posts.getPageable(), posts.getTotalElements());
        }
        List<Long> postIds = posts.getContent().stream().map(Post::getId).toList();
        List<Long> authorIds = posts.getContent().stream()
                .map(post -> post.getAuthor().getId()).distinct().toList();
        Map<Long, Long> likes = counts(likeRepository.countByPostIds(postIds));
        Map<Long, Long> comments = counts(commentRepository.countByPostIds(postIds));
        Set<Long> liked = postIds.isEmpty() ? Set.of()
                : Set.copyOf(likeRepository.findLikedPostIds(viewer.userId(), postIds));
        Set<Long> saved = postIds.isEmpty() ? Set.of()
                : Set.copyOf(savedPostRepository.findSavedPostIds(viewer.userId(), postIds));
        Set<Long> followed = authorIds.isEmpty() ? Set.of()
                : Set.copyOf(followRepository.findFollowedUserIds(viewer.userId(), authorIds));
        List<PostDto> content = posts.getContent().stream()
                .map(post -> postMapper.toDto(post, viewer,
                        likes.getOrDefault(post.getId(), 0L),
                        comments.getOrDefault(post.getId(), 0L),
                        liked.contains(post.getId()), saved.contains(post.getId()),
                        followed.contains(post.getAuthor().getId())))
                .toList();
        return new PageImpl<>(content, posts.getPageable(), posts.getTotalElements());
    }

    private PostDto toDto(Post post, AccountPrincipal viewer) {
        Long authorId = post.getAuthor().getId();
        return postMapper.toDto(post, viewer,
                likeRepository.countByPostId(post.getId()),
                commentRepository.countByPostId(post.getId()),
                likeRepository.existsByUserIdAndPostId(viewer.userId(), post.getId()),
                savedPostRepository.existsByUserIdAndPostId(viewer.userId(), post.getId()),
                followRepository.existsByFollowerIdAndFollowedId(viewer.userId(), authorId));
    }

    private void ensureVisible(Post post, Long viewerId) {
        ensureVisible(post.getAuthor(), viewerId);
    }

    private void ensureVisible(UserAccount author, Long viewerId) {
        if (!viewerId.equals(author.getId())
                && blockRepository.existsBlockBetween(viewerId, author.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found");
        }
        if (author.isPrivateAccount() && !author.getId().equals(viewerId)
                && !followRepository.existsByFollowerIdAndFollowedId(viewerId, author.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PRIVATE_ACCOUNT",
                    "Follow this account to view its posts");
        }
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post was not found"));
    }

    private UserAccount findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
    }

    private void publishActivity(Long actorId, Long recipientId, NotificationType type, Long postId, String preview) {
        if (!actorId.equals(recipientId)) {
            eventPublisher.publishEvent(new SocialActivityEvent(actorId, recipientId, type, postId, preview));
        }
    }

    private Map<Long, Long> counts(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1],
                (first, ignored) -> first));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
