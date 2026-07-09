package com.publication_trend_tracking_system.sever_web_app.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNoteRequest {

    private String note;
}