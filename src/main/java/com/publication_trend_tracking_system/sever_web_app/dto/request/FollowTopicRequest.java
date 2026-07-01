package com.publication_trend_tracking_system.sever_web_app.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowTopicRequest {

    private Integer topicId;
}