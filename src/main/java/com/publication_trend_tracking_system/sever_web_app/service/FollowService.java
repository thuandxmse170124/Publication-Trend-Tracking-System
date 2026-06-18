package com.publication_trend_tracking_system.sever_web_app.service;

import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowTopicRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FollowTopicResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowJournalRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FollowJournalResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowAuthorRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FollowAuthorResponse;
import java.util.List;

public interface FollowService {

    void followTopic(
            FollowTopicRequest request,
            String email);

    List<FollowTopicResponse> getMyFollowedTopics(
            String email);

    void unfollowTopic(
            String topicId,
            String email);

    void followJournal(
            FollowJournalRequest request,
            String email);

    List<FollowJournalResponse>
    getMyFollowedJournals(
            String email);

    void unfollowJournal(
            String journalId,
            String email);

    void followAuthor(
            FollowAuthorRequest request,
            String email);

    List<FollowAuthorResponse>
    getMyFollowedAuthors(
            String email);

    void unfollowAuthor(
            Long authorId,
            String email);
}