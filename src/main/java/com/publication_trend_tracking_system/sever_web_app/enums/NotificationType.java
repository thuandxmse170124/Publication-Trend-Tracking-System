package com.publication_trend_tracking_system.sever_web_app.enums;

/**
 * What a notification is about, and therefore where clicking it should go.
 *
 * <p>Before this existed every notification carried a bare {@code relatedId} that the frontend
 * always resolved as a paper id, so any notification about anything else was either a dead link or
 * a link to the wrong page. The type is what lets one feed hold several kinds of event.
 */
public enum NotificationType {

    /** N new papers were synced under a topic the user follows. relatedId = topicId. */
    NEW_PAPERS_IN_TOPIC,

    /** N new papers were synced from an author the user follows. relatedId = authorId. */
    NEW_PAPERS_BY_AUTHOR,

    /** N new papers were synced in a journal the user follows. relatedId = journalId. */
    NEW_PAPERS_IN_JOURNAL,

    /** A followed topic crossed the rapid-growth threshold. relatedId = topicId. */
    TOPIC_TREND
}
