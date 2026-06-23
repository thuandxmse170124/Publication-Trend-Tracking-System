package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowTopicResponse {

    private Long followId;

    private Integer topicId;

    private String topicName;
}