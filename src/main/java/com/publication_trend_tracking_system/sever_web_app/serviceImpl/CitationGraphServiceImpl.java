package com.publication_trend_tracking_system.sever_web_app.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.publication_trend_tracking_system.sever_web_app.client.OpenAlexClient;

import com.publication_trend_tracking_system.sever_web_app.dto.response.*;
import com.publication_trend_tracking_system.sever_web_app.entity.Paper;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.service.CitationGraphService;
import com.publication_trend_tracking_system.sever_web_app.service.citation.CitationRelationshipAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class CitationGraphServiceImpl implements CitationGraphService {

    private final PaperRepository paperRepository;
    private final OpenAlexClient openAlexClient;
    private final CitationRelationshipAnalyzer citationRelationshipAnalyzer;

    @Override
    @Transactional
    public CitationGraphResponse getCitationGraph(Long paperId) {

        // 1. Tìm paper trong DB
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() ->
                        new RuntimeException("Paper not found: " + paperId)
                );

        // 2. Lấy OpenAlex ID
        String openAlexId = resolveOpenAlexId(paper);

        // 3. Gọi OpenAlex lấy paper trung tâm
        JsonNode centerWork =
                openAlexClient.getWorkByOpenAlexId(openAlexId);

        if (centerWork == null) {
            throw new RuntimeException(
                    "Cannot get paper from OpenAlex"
            );
        }

        // 4. Lấy referenced_works
        JsonNode referencedWorks =
                centerWork.path("referenced_works");

        Map<String, CitationAuthorGroupResponse> authorGroupMap =
                new LinkedHashMap<>();

        int totalReferences = 0;

        // Bản demo giới hạn 20 references
        int maxReferences = 20;

        if (referencedWorks.isArray()) {

            for (JsonNode referenceNode : referencedWorks) {

                if (totalReferences >= maxReferences) {
                    break;
                }

                String referenceUrl =
                        referenceNode.asText();

                String citedOpenAlexId =
                        extractOpenAlexId(referenceUrl);

                try {

                    JsonNode citedWork =
                            openAlexClient
                                    .getWorkByOpenAlexId(
                                            citedOpenAlexId
                                    );

                    if (citedWork == null) {
                        continue;
                    }

                    addPaperToAuthorGroup(
                            citedWork,
                            authorGroupMap
                    );

                    totalReferences++;

                } catch (Exception e) {

                    System.out.println(
                            "Cannot load cited paper: "
                                    + citedOpenAlexId
                    );
                }
            }
        }

        // 5. Tạo center paper
        CitationCenterPaperResponse centerPaper =
                CitationCenterPaperResponse.builder()
                        .paperId(paper.getPaperId())
                        .openAlexId(openAlexId)
                        .title(paper.getTitle())
                        .publicationYear(
                                paper.getPublicationYear()
                        )
                        .citationCount(
                                paper.getCitationCount()
                        )
                        .build();

        // 6. Trả graph
        return CitationGraphResponse.builder()
                .centerPaper(centerPaper)
                .totalReferences(totalReferences)
                .authorGroups(
                        new ArrayList<>(
                                authorGroupMap.values()
                        )
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CitationPaperPreviewResponse getPaperPreview(
            String openAlexId
    ) {

        if (openAlexId == null || openAlexId.isBlank()) {
            throw new RuntimeException(
                    "OpenAlex ID must not be empty"
            );
        }

        JsonNode work =
                openAlexClient.getWorkByOpenAlexId(
                        openAlexId
                );

        if (work == null
                || work.path("id").asText().isBlank()) {

            throw new RuntimeException(
                    "Cannot find paper on OpenAlex: "
                            + openAlexId
            );
        }

        List<String> authors = new ArrayList<>();

        JsonNode authorships =
                work.path("authorships");

        if (authorships.isArray()) {

            for (JsonNode authorship : authorships) {

                String authorName =
                        authorship
                                .path("author")
                                .path("display_name")
                                .asText("");

                if (!authorName.isBlank()) {
                    authors.add(authorName);
                }
            }
        }

        return CitationPaperPreviewResponse.builder()
                .openAlexId(
                        extractOpenAlexId(
                                work.path("id").asText()
                        )
                )
                .title(
                        work.path("display_name")
                                .asText("")
                )
                .paperAbstract(
                        rebuildAbstract(
                                work.path(
                                        "abstract_inverted_index"
                                )
                        )
                )
                .publicationYear(
                        getNullableInteger(
                                work,
                                "publication_year"
                        )
                )
                .citedByCount(
                        getNullableInteger(
                                work,
                                "cited_by_count"
                        )
                )
                .doi(
                        getDoi(work)
                )
                .openAccess(
                        work.path("open_access")
                                .path("is_oa")
                                .asBoolean(false)
                )
                .authors(authors)
                .build();
    }

    /**
     * Nếu Paper đã có openAlexId:
     * → dùng luôn.
     *
     * Nếu chưa có:
     * → tìm OpenAlex bằng DOI
     * → lấy Wxxxx
     * → lưu DB.
     */
    private String resolveOpenAlexId(Paper paper) {

        // 1. Đã có OpenAlex ID → dùng ngay
        if (paper.getOpenAlexId() != null
                && !paper.getOpenAlexId().isBlank()) {

            return paper.getOpenAlexId();
        }

        // 2. Thử lấy OpenAlex ID từ source_url
        String sourceUrl = paper.getSourceUrl();

        if (sourceUrl != null
                && sourceUrl.contains("openalex.org/W")) {

            String openAlexId =
                    extractOpenAlexId(sourceUrl);

            paper.setOpenAlexId(openAlexId);
            paperRepository.save(paper);

            return openAlexId;
        }

        // 3. Không có source_url OpenAlex → tìm bằng DOI
        if (paper.getDoi() != null
                && !paper.getDoi().isBlank()) {

            JsonNode work =
                    openAlexClient.getWorkByDoi(
                            normalizeDoi(paper.getDoi())
                    );

            if (work == null
                    || work.path("id").isMissingNode()
                    || work.path("id").asText().isBlank()) {

                throw new RuntimeException(
                        "Cannot find paper on OpenAlex"
                );
            }

            String openAlexId =
                    extractOpenAlexId(
                            work.path("id").asText()
                    );

            paper.setOpenAlexId(openAlexId);
            paperRepository.save(paper);

            return openAlexId;
        }

        throw new RuntimeException(
                "Paper does not have OpenAlex ID, OpenAlex source URL, or DOI"
        );
    }
    /**
     * Gom cited paper theo first author.
     */
    private void addPaperToAuthorGroup(
            JsonNode citedWork,
            Map<String, CitationAuthorGroupResponse> groupMap
    ) {

        JsonNode authorships =
                citedWork.path("authorships");

        String authorId = "UNKNOWN";
        String authorName = "Unknown Author";

        if (authorships.isArray()
                && !authorships.isEmpty()) {

            JsonNode firstAuthor =
                    authorships.get(0)
                            .path("author");

            authorId =
                    extractOpenAlexId(
                            firstAuthor
                                    .path("id")
                                    .asText()
                    );

            authorName =
                    firstAuthor
                            .path("display_name")
                            .asText("Unknown Author");
        }

        CitationPaperNodeResponse paperNode =
                CitationPaperNodeResponse.builder()
                        .openAlexId(
                                extractOpenAlexId(
                                        citedWork
                                                .path("id")
                                                .asText()
                                )
                        )
                        .title(
                                citedWork
                                        .path("display_name")
                                        .asText()
                        )
                        .publicationYear(
                                getNullableInteger(
                                        citedWork,
                                        "publication_year"
                                )
                        )
                        .citedByCount(
                                getNullableInteger(
                                        citedWork,
                                        "cited_by_count"
                                )
                        )
                        .doi(
                                getDoi(citedWork)
                        )
                        .build();

        CitationAuthorGroupResponse group =
                groupMap.get(authorId);

        if (group == null) {

            List<CitationPaperNodeResponse> papers =
                    new ArrayList<>();

            papers.add(paperNode);

            group =
                    CitationAuthorGroupResponse.builder()
                            .authorId(authorId)
                            .authorName(authorName)
                            .paperCount(1)
                            .papers(papers)
                            .build();

            groupMap.put(authorId, group);

        } else {

            group.getPapers().add(paperNode);

            group.setPaperCount(
                    group.getPapers().size()
            );
        }
    }

    private String extractOpenAlexId(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        int lastSlash =
                value.lastIndexOf("/");

        if (lastSlash >= 0) {
            return value.substring(lastSlash + 1);
        }

        return value;
    }

    private String normalizeDoi(String doi) {

        return doi
                .replace("https://doi.org/", "")
                .replace("http://doi.org/", "")
                .trim();
    }

    private Integer getNullableInteger(
            JsonNode node,
            String fieldName
    ) {

        JsonNode value =
                node.get(fieldName);

        if (value == null || value.isNull()) {
            return null;
        }

        return value.asInt();
    }

    private String getDoi(JsonNode work) {

        JsonNode doiNode =
                work.get("doi");

        if (doiNode == null || doiNode.isNull()) {
            return null;
        }

        return doiNode
                .asText()
                .replace(
                        "https://doi.org/",
                        ""
                );
    }
    private String rebuildAbstract(
            JsonNode abstractInvertedIndex
    ) {

        if (abstractInvertedIndex == null
                || abstractInvertedIndex.isNull()
                || abstractInvertedIndex.isMissingNode()
                || !abstractInvertedIndex.isObject()) {

            return null;
        }

        Map<Integer, String> words =
                new java.util.TreeMap<>();

        abstractInvertedIndex
                .fields()
                .forEachRemaining(entry -> {

                    String word = entry.getKey();

                    JsonNode positions =
                            entry.getValue();

                    if (positions.isArray()) {

                        for (JsonNode position : positions) {

                            words.put(
                                    position.asInt(),
                                    word
                            );
                        }
                    }
                });

        if (words.isEmpty()) {
            return null;
        }

        return String.join(
                " ",
                words.values()
        );
    }
    @Override
    @Transactional(readOnly = true)
    public CitationAuthorPreviewResponse getAuthorPreview(
            String authorId
    ) {

        if (authorId == null || authorId.isBlank()) {
            throw new RuntimeException(
                    "OpenAlex Author ID must not be empty"
            );
        }

        JsonNode author =
                openAlexClient.getAuthorByOpenAlexId(
                        authorId
                );

        if (author == null
                || author.path("id").asText().isBlank()) {

            throw new RuntimeException(
                    "Cannot find author on OpenAlex: "
                            + authorId
            );
        }

        String institutionName = null;
        String institutionCountryCode = null;

        JsonNode lastKnownInstitutions =
                author.path("last_known_institutions");

        if (lastKnownInstitutions.isArray()
                && !lastKnownInstitutions.isEmpty()) {

            JsonNode institution =
                    lastKnownInstitutions.get(0);

            institutionName =
                    institution
                            .path("display_name")
                            .asText(null);

            institutionCountryCode =
                    institution
                            .path("country_code")
                            .asText(null);
        }

        List<String> researchTopics =
                new ArrayList<>();

        JsonNode topics =
                author.path("topics");

        if (topics.isArray()) {

            for (JsonNode topic : topics) {

                String topicName =
                        topic.path("display_name")
                                .asText("");

                if (!topicName.isBlank()) {
                    researchTopics.add(topicName);
                }

                // Hover card chỉ cần tối đa 5 topic
                if (researchTopics.size() >= 5) {
                    break;
                }
            }
        }

        return CitationAuthorPreviewResponse.builder()
                .openAlexAuthorId(
                        extractOpenAlexId(
                                author.path("id").asText()
                        )
                )
                .displayName(
                        author.path("display_name")
                                .asText("")
                )
                .orcid(
                        normalizeOrcid(
                                author.path("orcid")
                                        .asText(null)
                        )
                )
                .institutionName(institutionName)
                .institutionCountryCode(
                        institutionCountryCode
                )
                .worksCount(
                        getNullableInteger(
                                author,
                                "works_count"
                        )
                )
                .citedByCount(
                        getNullableInteger(
                                author,
                                "cited_by_count"
                        )
                )
                .researchTopics(researchTopics)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitationPaperNodeResponse> getReferences(
            Long paperId
    ) {

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() ->
                        new RuntimeException("Paper not found"));

        String openAlexId =
                resolveOpenAlexId(paper);

        return getReferencesByOpenAlexId(openAlexId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitationPaperNodeResponse> getCitedBy(
            Long paperId
    ) {

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() ->
                        new RuntimeException("Paper not found"));

        String openAlexId =
                resolveOpenAlexId(paper);

        return getCitedByOpenAlexId(openAlexId);
    }

    @Override
    @Transactional(readOnly = true)
    public CitationRelationshipResponse getRelationship(
            Long paperId,
            String referenceOpenAlexId
    ) {

        Paper paper =
                paperRepository.findById(paperId)
                        .orElseThrow(() ->
                                new RuntimeException("Paper not found"));

        String sourceOpenAlexId =
                resolveOpenAlexId(paper);

        JsonNode sourceWork =
                openAlexClient.getWorkByOpenAlexId(sourceOpenAlexId);

        JsonNode targetWork =
                openAlexClient.getWorkByOpenAlexId(referenceOpenAlexId);

        return citationRelationshipAnalyzer.analyze(
                sourceWork,
                targetWork
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResearchContextResponse getResearchContext(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));
        String openAlexId = resolveOpenAlexId(paper);
        JsonNode centerWork = openAlexClient.getWorkByOpenAlexId(openAlexId);
        if (centerWork == null) {
            throw new RuntimeException("Cannot get paper from OpenAlex");
        }

        return buildResearchContext(centerWork, getReferencesByOpenAlexId(openAlexId), getCitedByOpenAlexId(openAlexId));
    }

    @Override
    @Transactional(readOnly = true)
    public ResearchContextResponse getResearchContextByOpenAlexId(String openAlexId) {
        String normalizedId = extractOpenAlexId(openAlexId);
        JsonNode centerWork = openAlexClient.getWorkByOpenAlexId(normalizedId);
        if (centerWork == null || centerWork.path("id").asText().isBlank()) {
            throw new RuntimeException("Cannot find paper on OpenAlex: " + normalizedId);
        }
        return buildResearchContext(centerWork, getReferencesByOpenAlexId(normalizedId), getCitedByOpenAlexId(normalizedId));
    }

    private ResearchContextResponse buildResearchContext(JsonNode centerWork,
                                                          List<CitationPaperNodeResponse> references,
                                                          List<CitationPaperNodeResponse> citedBy) {
        references.sort(Comparator.comparing(CitationPaperNodeResponse::getCitedByCount,
                Comparator.nullsLast(Comparator.reverseOrder())));
        citedBy.sort(Comparator.comparing(CitationPaperNodeResponse::getCitedByCount,
                Comparator.nullsLast(Comparator.reverseOrder())));

        String researchArea = firstTopic(centerWork);

        List<CitationPaperNodeResponse> keyReferences = references.stream().limit(3).toList();
        List<CitationPaperNodeResponse> influencedStudies = citedBy.stream().limit(3).toList();
        List<ResearchPathStepResponse> path = buildReadingPath(keyReferences, buildPaperNode(centerWork), influencedStudies);
        List<CitationReasonResponse> reasons = buildCitationReasons(centerWork, citedBy);

        return ResearchContextResponse.builder()
                .researchArea(researchArea)
                .keyReferences(keyReferences)
                .readingPath(path)
                .influencedStudies(influencedStudies)
                .citationInsights(reasons)
                .build();
    }

    private List<CitationPaperNodeResponse> getReferencesByOpenAlexId(String openAlexId) {
        JsonNode work = openAlexClient.getWorkByOpenAlexId(openAlexId);
        List<CitationPaperNodeResponse> result = new ArrayList<>();
        JsonNode references = work.path("referenced_works");
        if (!references.isArray()) return result;

        for (JsonNode reference : references) {
            JsonNode referenceWork = openAlexClient.getWorkByOpenAlexId(extractOpenAlexId(reference.asText()));
            if (referenceWork != null) result.add(buildPaperNode(referenceWork));
        }
        return result;
    }

    private List<CitationPaperNodeResponse> getCitedByOpenAlexId(String openAlexId) {
        JsonNode response = openAlexClient.getCitedBy(openAlexId);
        List<CitationPaperNodeResponse> result = new ArrayList<>();
        JsonNode works = response.path("results");
        if (!works.isArray()) return result;

        for (JsonNode work : works) result.add(buildPaperNode(work));
        return result;
    }

    /** Backward-compatible adapter for the former Research Guide endpoint. */
    @Override
    @Transactional(readOnly = true)
    public ResearchGuideResponse getResearchGuide(Long paperId) {
        ResearchContextResponse context = getResearchContext(paperId);
        return ResearchGuideResponse.builder()
                .researchArea(context.getResearchArea())
                .keyReferences(context.getKeyReferences())
                .readingPath(context.getReadingPath())
                .influencedStudies(context.getInfluencedStudies())
                .citationInsights(context.getCitationInsights())
                .build();
    }

    private List<ResearchPathStepResponse> buildReadingPath(List<CitationPaperNodeResponse> foundations,
                                                             CitationPaperNodeResponse center,
                                                             List<CitationPaperNodeResponse> influenced) {
        List<ResearchPathStepResponse> path = new ArrayList<>();
        List<CitationPaperNodeResponse> candidates = new ArrayList<>();
        candidates.addAll(foundations);
        candidates.add(center);
        candidates.addAll(influenced);
        for (int i = 0; i < candidates.size(); i++) {
            boolean isReference = i < foundations.size();
            boolean isCurrent = i == foundations.size();
            String purpose = isReference ? "Review a key reference" :
                    isCurrent ? "Read the current paper" : "Explore a subsequent study";
            String reason = isReference ? "This paper is cited by the current paper and is useful context for the topic." :
                    isCurrent ? "Read this paper after its key references to understand its contribution." :
                            "This paper cites the current paper and shows how later work connects to it.";
            path.add(ResearchPathStepResponse.builder().order(i + 1)
                    .purpose(purpose).reason(reason)
                    .paper(candidates.get(i)).build());
        }
        return path;
    }

    private List<CitationReasonResponse> buildCitationReasons(JsonNode centerWork,
                                                                List<CitationPaperNodeResponse> citedBy) {
        List<CitationReasonResponse> reasons = new ArrayList<>();
        for (CitationPaperNodeResponse node : citedBy.stream().limit(3).toList()) {
            JsonNode citingWork = openAlexClient.getWorkByOpenAlexId(node.getOpenAlexId());
            if (citingWork == null) continue;
            CitationRelationshipResponse relationship = citationRelationshipAnalyzer.analyze(citingWork, centerWork);
            CitationEvidenceResponse sourceEvidence = relationship.getEvidence();
            String connectionStrength = toConnectionStrength(relationship.getScore());
            reasons.add(CitationReasonResponse.builder().citingPaper(node)
                    .relationship(toRelationship(connectionStrength))
                    .connectionStrength(connectionStrength)
                    .reason(buildCitationInsightReason(connectionStrength, sourceEvidence))
                    .evidence(CitationInsightEvidenceResponse.builder()
                            .sharedTopics(sourceEvidence == null ? List.of() : sourceEvidence.getSharedTopics())
                            .sharedConcepts(sourceEvidence == null ? List.of() : sourceEvidence.getSharedConcepts())
                            .sharedKeywords(sourceEvidence == null ? List.of() : sourceEvidence.getSharedKeywords())
                            .sharedAuthors(sourceEvidence == null ? List.of() : sourceEvidence.getSharedAuthors())
                            .build())
                    .build());
        }
        return reasons;
    }

    private String firstTopic(JsonNode work) {
        JsonNode topics = work.path("topics");
        if (topics.isArray() && !topics.isEmpty()) return topics.get(0).path("display_name").asText("Unclassified research branch");
        JsonNode concepts = work.path("concepts");
        if (concepts.isArray() && !concepts.isEmpty()) return concepts.get(0).path("display_name").asText("Unclassified research branch");
        return "Unclassified research branch";
    }

    private String toConnectionStrength(Integer score) {
        if (score == null || score < 40) return "VERY_LOW";
        if (score < 60) return "LOW";
        if (score < 80) return "MEDIUM";
        return "HIGH";
    }

    private String toRelationship(String connectionStrength) {
        return switch (connectionStrength) {
            case "HIGH" -> "Highly Related";
            case "MEDIUM" -> "Related";
            case "LOW" -> "Weakly Related";
            default -> "Low Related";
        };
    }

    private String buildCitationInsightReason(String connectionStrength,
                                              CitationEvidenceResponse evidence) {
        if (evidence == null) {
            return "The available metadata indicates a " + connectionStrength.toLowerCase().replace('_', ' ') + " connection.";
        }
        List<String> signals = new ArrayList<>();
        if (evidence.getSharedTopics() != null && !evidence.getSharedTopics().isEmpty()) signals.add("shared research topics");
        if (evidence.getSharedConcepts() != null && !evidence.getSharedConcepts().isEmpty()) signals.add("shared research concepts");
        if (evidence.getSharedKeywords() != null && !evidence.getSharedKeywords().isEmpty()) signals.add("shared keywords");
        if (evidence.getSharedAuthors() != null && !evidence.getSharedAuthors().isEmpty()) signals.add("shared authors");
        if (signals.isEmpty()) {
            return "The available metadata indicates a " + connectionStrength.toLowerCase().replace('_', ' ') + " connection.";
        }
        return "This is a " + connectionStrength.toLowerCase().replace('_', ' ') +
                " connection based on " + String.join(", ", signals) + ".";
    }

    private CitationPaperNodeResponse buildPaperNode(
            JsonNode work
    ) {

        return CitationPaperNodeResponse.builder()
                .openAlexId(
                        extractOpenAlexId(
                                work.path("id").asText()
                        )
                )
                .title(
                        work.path("display_name")
                                .asText("")
                )
                .publicationYear(
                        getNullableInteger(
                                work,
                                "publication_year"
                        )
                )
                .citedByCount(
                        getNullableInteger(
                                work,
                                "cited_by_count"
                        )
                )
                .doi(
                        getDoi(work)
                )
                .build();
    }



    private String normalizeOrcid(String orcid) {

        if (orcid == null || orcid.isBlank()) {
            return null;
        }

        return orcid
                .replace("https://orcid.org/", "")
                .trim();
    }
}
