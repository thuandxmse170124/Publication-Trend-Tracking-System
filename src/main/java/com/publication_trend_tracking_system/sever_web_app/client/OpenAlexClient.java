package com.publication_trend_tracking_system.sever_web_app.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OpenAlexClient {

    private static final String OPENALEX_BASE_URL =
            "https://api.openalex.org";

    private final RestClient.Builder restClientBuilder;

    /**
     * Tìm OpenAlex Work bằng DOI.
     *
     * Ví dụ DOI:
     * 10.1038/s41586-023-06647-8
     */
    public JsonNode getWorkByDoi(String doi) {

        RestClient restClient = restClientBuilder
                .baseUrl(OPENALEX_BASE_URL)
                .build();

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/works/https://doi.org/{doi}")
                        .build(doi))
                .retrieve()
                .body(JsonNode.class);
    }

    /**
     * Lấy Work bằng OpenAlex ID.
     *
     * Ví dụ:
     * W2122410182
     */
    public JsonNode getWorkByOpenAlexId(String openAlexId) {

        RestClient restClient = restClientBuilder
                .baseUrl(OPENALEX_BASE_URL)
                .build();

        return restClient.get()
                .uri("/works/{openAlexId}", openAlexId)
                .retrieve()
                .body(JsonNode.class);
    }
    /**
     * Lấy thông tin Author bằng OpenAlex Author ID.
     *
     * Ví dụ:
     * A5088275098
     */
    public JsonNode getAuthorByOpenAlexId(String authorId) {

        RestClient restClient = restClientBuilder
                .baseUrl(OPENALEX_BASE_URL)
                .build();

        return restClient.get()
                .uri("/authors/{authorId}", authorId)
                .retrieve()
                .body(JsonNode.class);
    }
}