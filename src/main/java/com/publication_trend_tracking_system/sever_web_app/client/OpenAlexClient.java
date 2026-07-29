package com.publication_trend_tracking_system.sever_web_app.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OpenAlexClient {

    private static final String BASE_URL = "https://api.openalex.org";

    private static final String WORKS = "/works";
    private static final String AUTHORS = "/authors";

    private final RestClient.Builder restClientBuilder;

    private RestClient client() {
        return restClientBuilder
                .baseUrl(BASE_URL)
                .build();
    }

    /*
     * ==========================
     * Work API
     * ==========================
     */

    public JsonNode getWorkByDoi(String doi) {
        try {
            return client()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(WORKS + "/https://doi.org/{doi}")
                            .build(doi))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode getWorkByOpenAlexId(String openAlexId) {
        try {
            return client()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(WORKS + "/{id}")
                            .build(openAlexId))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode searchWorks(String keyword) {
        try {
            return client()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(WORKS)
                            .queryParam("search", keyword)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    /*
     * ==========================
     * Author API
     * ==========================
     */

    public JsonNode getAuthorByOpenAlexId(String authorId) {
        try {
            return client()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(AUTHORS + "/{id}")
                            .build(authorId))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode getAuthorWorks(String authorId) {
        try {
            return client()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(WORKS)
                            .queryParam(
                                    "filter",
                                    "authorships.author.id:https://openalex.org/" + authorId
                            )
                            .queryParam("per-page", 20)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    /*
     * ==========================
     * Citation API
     * ==========================
     */

    public JsonNode getCitedBy(String openAlexId) {
        try {
            return client()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(WORKS)
                            .queryParam(
                                    "filter",
                                    "cites:https://openalex.org/" + openAlexId
                            )
                            .queryParam("per-page", 20)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }
}