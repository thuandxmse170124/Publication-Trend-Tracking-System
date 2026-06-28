package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalResponse {
    private Integer journalId;
    private String name;
    private String issn;
    private String publisher;
    private String status;
}
