package com.publication_trend_tracking_system.sever_web_app.service.citation;

import com.fasterxml.jackson.databind.JsonNode;
import com.publication_trend_tracking_system.sever_web_app.dto.response.CitationEvidenceResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.CitationRelationshipResponse;
import com.publication_trend_tracking_system.sever_web_app.dto.response.CitationSimilarityResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CitationRelationshipAnalyzer {

    /**
     * Weight của từng tiêu chí.
     * Tổng = 1.0
     */
    private static final double TOPIC_WEIGHT = 0.35;
    private static final double CONCEPT_WEIGHT = 0.20;
    private static final double KEYWORD_WEIGHT = 0.15;
    private static final double AUTHOR_WEIGHT = 0.15;
    private static final double JOURNAL_WEIGHT = 0.10;
    private static final double YEAR_WEIGHT = 0.05;

    public CitationRelationshipResponse analyze(
            JsonNode sourceWork,
            JsonNode targetWork
    ) {

        List<String> matchedTopics =
                getMatchedTopics(sourceWork, targetWork);

        List<String> matchedConcepts =
                getMatchedConcepts(sourceWork, targetWork);

        List<String> matchedKeywords =
                getMatchedKeywords(sourceWork, targetWork);

        List<String> matchedAuthors =
                getMatchedAuthors(sourceWork, targetWork);

        boolean sameJournal =
                isSameJournal(sourceWork, targetWork);

        double topicSimilarity =
                calculateTopicSimilarity(sourceWork, targetWork);

        double conceptSimilarity =
                calculateConceptSimilarity(sourceWork, targetWork);

        double keywordSimilarity =
                calculateKeywordSimilarity(sourceWork, targetWork);

        double authorSimilarity =
                calculateAuthorSimilarity(sourceWork, targetWork);

        double journalSimilarity =
                calculateJournalSimilarity(sourceWork, targetWork);

        double publicationYearSimilarity =
                calculatePublicationYearSimilarity(sourceWork, targetWork);

        int score =
                calculateRelationshipScore(
                        topicSimilarity,
                        conceptSimilarity,
                        keywordSimilarity,
                        authorSimilarity,
                        journalSimilarity,
                        publicationYearSimilarity
                );

        CitationSimilarityResponse similarity =
                buildSimilarity(
                        topicSimilarity,
                        conceptSimilarity,
                        keywordSimilarity,
                        authorSimilarity,
                        journalSimilarity,
                        publicationYearSimilarity
                );

        CitationEvidenceResponse evidence =
                buildEvidence(
                        sourceWork,
                        targetWork,
                        matchedTopics,
                        matchedConcepts,
                        matchedKeywords,
                        matchedAuthors,
                        sameJournal
                );

        String relationshipRole =
                determineRelationshipRole(
                        evidence
                );

        String relationshipDescription =
                buildRelationshipDescription(
                        relationshipRole,
                        evidence
                );

        return CitationRelationshipResponse.builder()
                .score(score)
                .relationship(resolveRelationship(score))
                .relationshipRole(relationshipRole)
                .relationshipDescription(relationshipDescription)
                .similarity(similarity)
                .evidence(evidence)
                .build();
    }

    private CitationSimilarityResponse buildSimilarity(
            double topicSimilarity,
            double conceptSimilarity,
            double keywordSimilarity,
            double authorSimilarity,
            double journalSimilarity,
            double publicationYearSimilarity
    ) {

        return CitationSimilarityResponse.builder()
                .topicSimilarity((int) Math.round(topicSimilarity * 100))
                .conceptSimilarity((int) Math.round(conceptSimilarity * 100))
                .keywordSimilarity((int) Math.round(keywordSimilarity * 100))
                .authorSimilarity((int) Math.round(authorSimilarity * 100))
                .journalSimilarity((int) Math.round(journalSimilarity * 100))
                .publicationYearSimilarity((int) Math.round(publicationYearSimilarity * 100))
                .build();
    }

    private CitationEvidenceResponse buildEvidence(
            JsonNode sourceWork,
            JsonNode targetWork,
            List<String> matchedTopics,
            List<String> matchedConcepts,
            List<String> matchedKeywords,
            List<String> matchedAuthors,
            boolean sameJournal
    ) {

        String sourceJournal =
                sourceWork.path("primary_location")
                        .path("source")
                        .path("display_name")
                        .asText("");

        String targetJournal =
                targetWork.path("primary_location")
                        .path("source")
                        .path("display_name")
                        .asText("");

        int sourceYear =
                sourceWork.path("publication_year")
                        .asInt();

        int targetYear =
                targetWork.path("publication_year")
                        .asInt();

        return CitationEvidenceResponse.builder()
                .sharedTopics(matchedTopics)
                .sharedConcepts(matchedConcepts)
                .sharedKeywords(matchedKeywords)
                .sharedAuthors(matchedAuthors)
                .sameJournal(sameJournal)
                .sourceJournalName(sourceJournal)
                .targetJournalName(targetJournal)
                .sourcePublicationYear(sourceYear)
                .targetPublicationYear(targetYear)
                .publicationYearDifference(
                        Math.abs(sourceYear - targetYear)
                )
                .build();
    }
    /**
     * Lấy display_name thành Set<String>
     */
    private Set<String> extractDisplayNames(JsonNode arrayNode) {

        Set<String> values = new HashSet<>();

        if (!arrayNode.isArray()) {
            return values;
        }

        for (JsonNode node : arrayNode) {

            String value =
                    node.path("display_name")
                            .asText("");

            if (!value.isBlank()) {
                values.add(value);
            }
        }

        return values;
    }

    /**
     * Lấy Author ID
     */
    private Set<String> extractAuthorIds(JsonNode authorships) {

        Set<String> authors = new HashSet<>();

        if (!authorships.isArray()) {
            return authors;
        }

        for (JsonNode authorship : authorships) {

            String authorId =
                    authorship.path("author")
                            .path("id")
                            .asText("");

            if (!authorId.isBlank()) {
                authors.add(authorId);
            }
        }

        return authors;
    }

    /**
     * Tính Jaccard Similarity
     */
    private double calculateJaccardSimilarity(
            Set<String> source,
            Set<String> target
    ) {

        if (source.isEmpty() && target.isEmpty()) {
            return 0;
        }

        Set<String> intersection =
                new HashSet<>(source);

        intersection.retainAll(target);

        Set<String> union =
                new HashSet<>(source);

        union.addAll(target);

        return (double) intersection.size()
                / union.size();
    }

    /**
     * Trả về phần tử giao nhau
     */
    private List<String> getIntersection(
            Set<String> source,
            Set<String> target
    ) {

        Set<String> intersection =
                new HashSet<>(source);

        intersection.retainAll(target);

        return new ArrayList<>(intersection);
    }


    private double calculateTopicSimilarity(
            JsonNode source,
            JsonNode target
    ) {

        return calculateJaccardSimilarity(
                extractDisplayNames(source.path("topics")),
                extractDisplayNames(target.path("topics"))
        );
    }

    private double calculateConceptSimilarity(
            JsonNode source,
            JsonNode target
    ) {

        return calculateJaccardSimilarity(
                extractDisplayNames(source.path("concepts")),
                extractDisplayNames(target.path("concepts"))
        );
    }

    private double calculateKeywordSimilarity(
            JsonNode source,
            JsonNode target
    ) {

        return calculateJaccardSimilarity(
                extractDisplayNames(source.path("keywords")),
                extractDisplayNames(target.path("keywords"))
        );
    }

    private double calculateAuthorSimilarity(
            JsonNode source,
            JsonNode target
    ) {

        return calculateJaccardSimilarity(
                extractAuthorIds(source.path("authorships")),
                extractAuthorIds(target.path("authorships"))
        );
    }

    private double calculateJournalSimilarity(
            JsonNode source,
            JsonNode target
    ) {

        String sourceJournal =
                source.path("primary_location")
                        .path("source")
                        .path("display_name")
                        .asText("");

        String targetJournal =
                target.path("primary_location")
                        .path("source")
                        .path("display_name")
                        .asText("");

        if (sourceJournal.isBlank()
                || targetJournal.isBlank()) {
            return 0;
        }

        return sourceJournal.equalsIgnoreCase(targetJournal)
                ? 1.0
                : 0.0;
    }

    private double calculatePublicationYearSimilarity(
            JsonNode source,
            JsonNode target
    ) {

        int sourceYear =
                source.path("publication_year")
                        .asInt(0);

        int targetYear =
                target.path("publication_year")
                        .asInt(0);

        if (sourceYear == 0 || targetYear == 0) {
            return 0;
        }

        int difference =
                Math.abs(sourceYear - targetYear);

        return Math.max(
                0,
                1 - (difference / 10.0)
        );
    }

    private int calculateRelationshipScore(
            double topicSimilarity,
            double conceptSimilarity,
            double keywordSimilarity,
            double authorSimilarity,
            double journalSimilarity,
            double publicationYearSimilarity
    ){
        if (topicSimilarity == 0
                && conceptSimilarity == 0
                && keywordSimilarity == 0
                && authorSimilarity == 0
                && journalSimilarity == 0) {

            return 0;
        }

        double weightedScore =
                topicSimilarity * TOPIC_WEIGHT
                        + conceptSimilarity * CONCEPT_WEIGHT
                        + keywordSimilarity * KEYWORD_WEIGHT
                        + authorSimilarity * AUTHOR_WEIGHT
                        + journalSimilarity * JOURNAL_WEIGHT
                        + publicationYearSimilarity * YEAR_WEIGHT;

        return (int) Math.round(weightedScore * 100);
    }

    private String resolveRelationship(
            int score
    ) {

        if (score >= 80) {
            return "Highly Related";
        }

        if (score >= 60) {
            return "Related";
        }

        if (score >= 40) {
            return "Weakly Related";
        }

        return "Low Related";
    }



    private List<String> getMatchedTopics(
            JsonNode source,
            JsonNode target
    ) {

        return getIntersection(
                extractDisplayNames(source.path("topics")),
                extractDisplayNames(target.path("topics"))
        );
    }

    private List<String> getMatchedConcepts(
            JsonNode source,
            JsonNode target
    ) {

        return getIntersection(
                extractDisplayNames(source.path("concepts")),
                extractDisplayNames(target.path("concepts"))
        );
    }

    private List<String> getMatchedKeywords(
            JsonNode source,
            JsonNode target
    ) {

        return getIntersection(
                extractDisplayNames(source.path("keywords")),
                extractDisplayNames(target.path("keywords"))
        );
    }

    private List<String> getMatchedAuthors(
            JsonNode source,
            JsonNode target
    ) {

        Map<String, String> sourceAuthors =
                new HashMap<>();

        JsonNode sourceAuthorships =
                source.path("authorships");

        if (sourceAuthorships.isArray()) {

            for (JsonNode authorship : sourceAuthorships) {

                String id =
                        authorship.path("author")
                                .path("id")
                                .asText("");

                String name =
                        authorship.path("author")
                                .path("display_name")
                                .asText("");

                if (!id.isBlank()) {
                    sourceAuthors.put(id, name);
                }
            }
        }

        List<String> matched =
                new ArrayList<>();

        JsonNode targetAuthorships =
                target.path("authorships");

        if (targetAuthorships.isArray()) {

            for (JsonNode authorship : targetAuthorships) {

                String id =
                        authorship.path("author")
                                .path("id")
                                .asText("");

                if (sourceAuthors.containsKey(id)) {

                    matched.add(
                            sourceAuthors.get(id)
                    );
                }
            }
        }

        return matched;
    }



    private String determineRelationshipRole(
            CitationEvidenceResponse evidence
    ) {

        if (!evidence.getSharedTopics().isEmpty()
                && !evidence.getSharedConcepts().isEmpty()) {
            return "Strong Research Connection";
        }

        if (!evidence.getSharedTopics().isEmpty()) {
            return "Same Research Topic";
        }

        if (!evidence.getSharedConcepts().isEmpty()) {
            return "Shared Research Concepts";
        }

        if (!evidence.getSharedAuthors().isEmpty()) {
            return "Same Research Group";
        }

        if (Boolean.TRUE.equals(evidence.getSameJournal())) {
            return "Same Publication Venue";
        }

        return "Weak Research Connection";
    }


    private String buildRelationshipDescription(
            String role,
            CitationEvidenceResponse evidence
    ) {

        switch (role) {

            case "Strong Research Connection":
                return "The papers share multiple research topics and concepts, indicating a strong research connection.";

            case "Same Research Topic":
                return "The papers focus on similar research topics.";

            case "Shared Research Concepts":
                return "The papers share several important research concepts.";

            case "Same Research Group":
                return "The papers involve one or more common authors.";

            case "Same Publication Venue":
                return "The papers were published in the same journal.";

            default:
                return "The citation indicates a weak research connection based on the available metadata.";
        }
    }
    private boolean isSameJournal(
            JsonNode source,
            JsonNode target
    ) {

        return calculateJournalSimilarity(
                source,
                target
        ) == 1.0;
    }




}