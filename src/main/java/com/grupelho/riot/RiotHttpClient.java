package com.grupelho.riot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RiotHttpClient {

    private final HttpClient http;
    private final String apiKey;

    public RiotHttpClient(String apiKey) {
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String get(String url) {
        return send(HttpRequest.newBuilder(URI.create(url))
                .header("X-Riot-Token", apiKey)
                .GET());
    }

    public String post(String url, String jsonBody) {
        return send(HttpRequest.newBuilder(URI.create(url))
                .header("X-Riot-Token", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)));
    }

    private String send(HttpRequest.Builder builder) {
        HttpRequest request = builder.timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RiotApiException("Falha de rede ao chamar " + request.uri(), e);
        }

        if (response.statusCode() == 429) {
            String retryAfter = response.headers().firstValue("Retry-After").orElse("desconhecido");
            throw new RiotApiException("Rate limit da Riot API atingido (429). Retry-After: "
                    + retryAfter + "s. URL: " + request.uri());
        }

        if (response.statusCode() >= 400) {
            throw new RiotApiException("Riot API respondeu " + response.statusCode()
                    + " para " + request.uri() + ": " + response.body());
        }

        return response.body();
    }
}
