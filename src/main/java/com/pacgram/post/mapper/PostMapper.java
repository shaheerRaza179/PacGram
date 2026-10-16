package com.pacgram.post.mapper;

import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.post.dto.CommentDto;
import com.pacgram.post.dto.PostDto;
import com.pacgram.post.entity.Comment;
import com.pacgram.post.entity.Post;
import com.pacgram.user.mapper.UserMapper;

public class PostMapper {
    private final UserMapper userMapper = new UserMapper();

    public PostDto toDto(
            Post post, AccountPrincipal viewer, long likesCount, long commentsCount,
            boolean likedByViewer, boolean savedByViewer, boolean followedByViewer) {
        return new PostDto(post.getId(),
                userMapper.toSummary(post.getAuthor(), followedByViewer),
                post.getCaption(), post.getImageUrl(), likesCount, commentsCount,
                likedByViewer, savedByViewer, post.getCreatedAt());
    }

    public CommentDto toDto(Comment comment, boolean followedByViewer) {
        return new CommentDto(comment.getId(),
                userMapper.toSummary(comment.getAuthor(), followedByViewer),
                comment.getText(), comment.getCreatedAt());
    }
}
