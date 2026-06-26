package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowTopicRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FollowTopicResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.*;
import com.publication_trend_tracking_system.sever_web_app.repository.*;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowAuthorRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FollowAuthorResponse;
import com.publication_trend_tracking_system.sever_web_app.service.FollowService;
import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowJournalRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FollowJournalResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.Topic;
import com.publication_trend_tracking_system.sever_web_app.entity.Journal;
import com.publication_trend_tracking_system.sever_web_app.repository.TopicRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl
        implements FollowService {
    private final TopicRepository
            topicRepository;
    private final JournalRepository
            journalRepository;
    private final FollowTopicRepository
            followTopicRepository;
    private final FollowJournalRepository
            followJournalRepository;
    private final UserRepository
            userRepository;
    private final FollowAuthorRepository
            followAuthorRepository;
    private final AuthorRepository
            authorRepository;

    @Override
    public void followTopic(
            FollowTopicRequest request,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        boolean exists =
                followTopicRepository
                        .existsByUserUserIdAndTopicTopicId(
                                user.getUserId(),
                                request.getTopicId());

        if (exists) {

            throw new AppException(
                    ErrorCode.TOPIC_ALREADY_FOLLOWED);
        }

        Topic topic =
                topicRepository
                        .findById(
                                request.getTopicId())
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.TOPIC_NOT_FOUND));

        FollowTopic followTopic =
                FollowTopic.builder()
                        .topic(topic)
                        .user(user)
                        .build();

        followTopicRepository.save(
                followTopic);
    }

    @Override
    public List<FollowTopicResponse>
    getMyFollowedTopics(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        return followTopicRepository
                .findByUserUserId(
                        user.getUserId())
                .stream()
                .map(topic ->
                        FollowTopicResponse.builder()
                                .followId(
                                        topic.getFollowId())
                                .topicId(
                                        topic.getTopic()
                                                .getTopicId())
                                .topicName(
                                        topic.getTopic()
                                                .getTopicName())
                                .build())
                .toList();
    }

    @Override
    @Transactional
    public void unfollowTopic(
            Integer topicId,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));
        boolean exists =
                followTopicRepository
                        .existsByUserUserIdAndTopicTopicId(
                                user.getUserId(),
                                topicId);
        if (!exists) {

            throw new AppException(
                    ErrorCode.TOPIC_NOT_FOLLOWED);
        }
        followTopicRepository
                .deleteByUserUserIdAndTopicTopicId(
                        user.getUserId(),
                        topicId);
    }
    @Override
    public void followJournal(
            FollowJournalRequest request,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        boolean exists =
                followJournalRepository
                        .existsByUserUserIdAndJournalJournalId(
                                user.getUserId(),
                                request.getJournalId());

        if (exists) {

            throw new AppException(
                    ErrorCode.JOURNAL_ALREADY_FOLLOWED);
        }
        Journal journal =
                journalRepository
                        .findById(
                                request.getJournalId())
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.JOURNAL_NOT_FOUND));
        FollowJournal followJournal =
                FollowJournal.builder()
                        .journal(journal)
                        .user(user)
                        .build();

        followJournalRepository.save(
                followJournal);
    }
    @Override
    public List<FollowJournalResponse>
    getMyFollowedJournals(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        return followJournalRepository
                .findByUserUserId(
                        user.getUserId())
                .stream()
                .map(journal ->
                        FollowJournalResponse.builder()
                                .followId(
                                        journal.getFollowId())
                                .journalId(
                                        journal.getJournal()
                                                .getJournalId())
                                .journalName(
                                        journal.getJournal()
                                                .getName())
                                .build())
                .toList();
    }
    @Override
    @Transactional
    public void unfollowJournal(
            Integer journalId,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));
        boolean exists =
                followJournalRepository
                        .existsByUserUserIdAndJournalJournalId(
                                user.getUserId(),
                                journalId);

        if (!exists) {

            throw new AppException(
                    ErrorCode.JOURNAL_NOT_FOLLOWED);
        }
        followJournalRepository
                .deleteByUserUserIdAndJournalJournalId(
                        user.getUserId(),
                        journalId);
    }
    @Override
    public void followAuthor(
            FollowAuthorRequest request,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        boolean exists =
                followAuthorRepository
                        .existsByUserUserIdAndAuthorAuthorId(
                                user.getUserId(),
                                request.getAuthorId());

        if (exists) {

            throw new AppException(
                    ErrorCode.AUTHOR_ALREADY_FOLLOWED);
        }

        Author author =
                authorRepository
                        .findById(
                                request.getAuthorId())
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.AUTHOR_NOT_FOUND));

        FollowAuthor followAuthor =
                FollowAuthor.builder()
                        .author(author)
                        .user(user)
                        .build();

        followAuthorRepository.save(
                followAuthor);
    }
    @Override
    public List<FollowAuthorResponse>
    getMyFollowedAuthors(
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        return followAuthorRepository
                .findByUserUserId(
                        user.getUserId())
                .stream()
                .map(author ->
                        FollowAuthorResponse.builder()
                                .followId(
                                        author.getFollowId())
                                .authorId(
                                        author.getAuthor()
                                                .getAuthorId())
                                .authorName(
                                        author.getAuthor()
                                                .getFullName())
                                .build())
                .toList();
    }
    @Override
    @Transactional
    public void unfollowAuthor(
            Long authorId,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));

        boolean exists =
                followAuthorRepository
                        .existsByUserUserIdAndAuthorAuthorId(
                                user.getUserId(),
                                authorId);

        if (!exists) {

            throw new AppException(
                    ErrorCode.AUTHOR_NOT_FOLLOWED);
        }

        followAuthorRepository
                .deleteByUserUserIdAndAuthorAuthorId(
                        user.getUserId(),
                        authorId);
    }
}