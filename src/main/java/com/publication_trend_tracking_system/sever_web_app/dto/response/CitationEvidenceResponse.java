package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationEvidenceResponse {

    private List<String> sharedTopics;

    private List<String> sharedConcepts;

    private List<String> sharedKeywords;

    private List<String> sharedAuthors;

    private Boolean sameJournal;

    private String sourceJournalName;

    private String targetJournalName;

    private Integer sourcePublicationYear;

    private Integer targetPublicationYear;

    private Integer publicationYearDifference;

}