package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CitationRelationshipResponse {

    /**
     * Relationship Score (0-100)
     */
    private Integer score;

    /**
     * Strong / Medium / Weak
     */
    private String relationship;

    /**
     * Foundation
     * Methodology
     * Extension
     * Validation
     * Comparison
     */
    private String relationshipRole;

    /**
     * AI hoặc Rule-based giải thích
     */
    private String relationshipDescription;

    /**
     * Dùng cho FE hiển thị chi tiết (tuỳ chọn)
     */
    private CitationSimilarityResponse similarity;

    /**
     * Evidence để chứng minh
     */
    private CitationEvidenceResponse evidence;
}