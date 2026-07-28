package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PersonalStatsResponse {
    long bookmarks;
    long followingTopics;
    long followingAuthors;
    long followingJournals;
    long unreadNotifications;
    List<String> recentSearches;
}
