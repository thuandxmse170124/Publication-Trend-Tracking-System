package com.publication_trend_tracking_system.sever_web_app.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YearCountResponse {
    Integer year;
    Long count;
}
