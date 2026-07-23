package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicSeedStatusResponse {

    private long officialTopicCount;

    // OpenAlex's published taxonomy size as of Implementation Plan v3 (4,516 topics). Used by
    // the frontend only as a rough "X / total" progress hint — the real taxonomy can grow over
    // time, so this is not enforced anywhere server-side.
    private long expectedTopicCount;

    private long domainCount;

    private long fieldCount;

    private long subfieldCount;
}
