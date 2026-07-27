package com.seatech.minsu.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatech.minsu.service.RateFetcher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** open.er-api.com 汇率源（公开免费无密钥） */
@Component
public class ErApiRateFetcher implements RateFetcher {

    private static final String API_URL = "https://open.er-api.com/v6/latest/";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, BigDecimal> fetchRates(String baseCurrency) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL + baseCurrency))
                .timeout(TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("汇率 API HTTP " + response.statusCode());
        }
        return parse(response.body());
    }

    /** 解析 er-api 响应: {"result":"success","rates":{"USD":0.1394,...}} */
    public Map<String, BigDecimal> parse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        if (!"success".equalsIgnoreCase(root.path("result").asText())) {
            throw new IllegalStateException("汇率 API 返回失败: " + root.path("result").asText());
        }
        JsonNode rates = root.path("rates");
        if (!rates.isObject()) {
            throw new IllegalStateException("汇率 API 响应缺少 rates 字段");
        }
        Map<String, BigDecimal> map = new HashMap<>();
        rates.fields().forEachRemaining(entry -> {
            if (entry.getValue().isNumber()) {
                map.put(entry.getKey(), entry.getValue().decimalValue());
            }
        });
        return map;
    }
}
