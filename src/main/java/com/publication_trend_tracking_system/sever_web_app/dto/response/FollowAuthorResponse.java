package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowAuthorResponse {

    private Long followId;

    private Long authorId;

    private String authorName;
}