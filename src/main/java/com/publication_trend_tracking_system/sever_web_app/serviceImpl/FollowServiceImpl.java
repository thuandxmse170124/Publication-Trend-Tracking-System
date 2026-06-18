package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowTopicRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FollowTopicResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.FollowTopic;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.entity.Author;
import com.publication_trend_tracking_system.sever_web_app.repository.AuthorRepository;
import com.publication_trend_tracking_system.sever_web_app.exception.AppException;
import com.publication_trend_tracking_system.sever_web_app.exception.ErrorCode;
import com.publication_trend_tracking_system.sever_web_app.repository.FollowTopicRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowAuthorRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FollowAuthorResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.FollowAuthor;
import com.publication_trend_tracking_system.sever_web_app.repository.FollowAuthorRepository;
import com.publication_trend_tracking_system.sever_web_app.service.FollowService;
import com.publication_trend_tracking_system.sever_web_app.dto.request.FollowJournalRequest;
import com.publication_trend_tracking_system.sever_web_app.dto.response.FollowJournalResponse;
import com.publication_trend_tracking_system.sever_web_app.entity.FollowJournal;
import com.publication_trend_tracking_system.sever_web_app.repository.FollowJournalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl
        implements FollowService {

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
                        .existsByUserUserIdAndTopicId(
                                user.getUserId(),
                                request.getTopicId());

        if (exists) {

            throw new AppException(
                    ErrorCode.TOPIC_ALREADY_FOLLOWED);
        }

        FollowTopic followTopic =
                FollowTopic.builder()
                        .topicId(
                                request.getTopicId())
                        .topicName(
                                request.getTopicName())
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
                                        topic.getTopicId())
                                .topicName(
                                        topic.getTopicName())
                                .build())
                .toList();
    }

    @Override
    @Transactional
    public void unfollowTopic(
            String topicId,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));
        boolean exists =
                followTopicRepository
                        .existsByUserUserIdAndTopicId(
                                user.getUserId(),
                                topicId);

        if (!exists) {

            throw new AppException(
                    ErrorCode.TOPIC_NOT_FOLLOWED);
        }
        followTopicRepository
                .deleteByUserUserIdAndTopicId(
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
                        .existsByUserUserIdAndJournalId(
                                user.getUserId(),
                                request.getJournalId());

        if (exists) {

            throw new AppException(
                    ErrorCode.JOURNAL_ALREADY_FOLLOWED);
        }

        FollowJournal followJournal =
                FollowJournal.builder()
                        .journalId(
                                request.getJournalId())
                        .journalName(
                                request.getJournalName())
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
                                        journal.getJournalId())
                                .journalName(
                                        journal.getJournalName())
                                .build())
                .toList();
    }
    @Override
    @Transactional
    public void unfollowJournal(
            String journalId,
            String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new AppException(
                                        ErrorCode.USER_NOT_FOUND));
        boolean exists =
                followJournalRepository
                        .existsByUserUserIdAndJournalId(
                                user.getUserId(),
                                journalId);

        if (!exists) {

            throw new AppException(
                    ErrorCode.JOURNAL_NOT_FOLLOWED);
        }
        followJournalRepository
                .deleteByUserUserIdAndJournalId(
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