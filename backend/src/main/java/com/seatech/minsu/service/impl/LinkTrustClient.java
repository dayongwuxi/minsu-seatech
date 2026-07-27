package com.seatech.minsu.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatech.minsu.config.LinkTrustConfig;
import com.seatech.minsu.dto.LinkTrustTxn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * LinkTrust Pay HTTP 客户端（无官方 SDK，JDK HttpClient 手写，仿 OllamaCloudClient）。
 * 仅一个接口：POST /webpay/transactions 交易反查（IPN 无签名，其内容不可信，一律以反查为准）。
 * 解析逻辑抽为可单测的静态方法。
 */
@Slf4j
@Component
public class LinkTrustClient {

    private final LinkTrustConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LinkTrustClient(LinkTrustConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** 反查交易状态；网络/HTTP 异常上抛由调用方决定重试语义（IPN 场景回 500 触发渠道重发） */
    public LinkTrustTxn queryTransaction(String merOrderId) throws Exception {
        String body = buildQueryBody(config.getMerId(), merOrderId);
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.getQueryUrl()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("LinkTrust transactions HTTP " + resp.statusCode());
        }
        return parseTxnResponse(resp.body(), objectMapper);
    }

    /**
     * 构造反查请求体：表单编码 merId=..&merOrderId=..
     * （2026-07-24 实测：渠道仅接受 form 参数，官方 PDF 的 JSON Request 示例会被 400 拒绝，
     * 与 vendor sample code 的 HttpUtil.post(Map) 行为一致）
     */
    static String buildQueryBody(String merId, String merOrderId) {
        return "merId=" + URLEncoder.encode(merId, StandardCharsets.UTF_8)
                + "&merOrderId=" + URLEncoder.encode(merOrderId, StandardCharsets.UTF_8);
    }

    /** 解析反查响应；amount 官方示例为字符串（"3000"），兼容数值型，非法为 null */
    static LinkTrustTxn parseTxnResponse(String json, ObjectMapper om) throws Exception {
        JsonNode root = om.readTree(json);
        LinkTrustTxn txn = new LinkTrustTxn();
        txn.setTransactionId(root.path("transactionId").asText(null));
        txn.setResult(root.path("result").asText(null));
        txn.setPayMethod(root.path("payMethod").asText(null));
        JsonNode amount = root.path("amount");
        if (amount.isNumber()) {
            txn.setAmount(amount.asLong());
        } else if (amount.isTextual()) {
            try {
                txn.setAmount(Long.parseLong(amount.asText().trim()));
            } catch (NumberFormatException ignored) {
                // 非法金额视为缺失
            }
        }
        return txn;
    }

    /** result → 渠道状态：success=1 成功，failure/error=2 失败，pending/not found/其他=0 处理中 */
    public static int mapResultStatus(String result) {
        if ("success".equals(result)) {
            return 1;
        }
        if ("failure".equals(result) || "error".equals(result)) {
            return 2;
        }
        return 0;
    }

    /** 渠道支付方式 → pay_method 编码：wechatpay=1 alipay=2 unionpay=7；未知返回 null */
    public static Integer mapPayMethodCode(String payMethod) {
        if (payMethod == null) {
            return null;
        }
        return switch (payMethod) {
            case "wechatpay" -> 1;
            case "alipay" -> 2;
            case "unionpay" -> 7;
            default -> null;
        };
    }
}
