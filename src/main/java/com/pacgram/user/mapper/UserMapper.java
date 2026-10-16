package com.pacgram.user.mapper;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.user.dto.UserProfileDto;
import com.pacgram.user.dto.UserSummaryDto;

public class UserMapper {

    public UserSummaryDto toSummary(UserAccount user, boolean followedByViewer) {
        return new UserSummaryDto(user.getId(), user.getName(), user.getUsername(),
                user.getAvatarUrl(), user.getBio(), followedByViewer);
    }

    public UserProfileDto toProfile(
            UserAccount user,
            boolean ownProfile,
            boolean followedByViewer,
            long followersCount,
            long followingCount,
            long postsCount) {
        return new UserProfileDto(user.getId(), user.getName(), user.getUsername(),
                ownProfile ? user.getEmail() : null,
                user.getAvatarUrl(), user.getCoverUrl(), user.getBio(), user.isPrivateAccount(),
                followedByViewer, followersCount, followingCount, postsCount, user.getCreatedAt());
    }
}
