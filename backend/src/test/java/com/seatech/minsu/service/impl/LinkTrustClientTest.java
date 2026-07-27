package com.seatech.minsu.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatech.minsu.dto.LinkTrustTxn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** LinkTrust 客户端纯逻辑测试：查询请求体构造 + 响应解析 + 状态/方式映射（无需网络） */
class LinkTrustClientTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("查询请求体：表单编码（实测渠道仅收 form 参数，PDF 的 JSON 示例不可用）")
    void buildsFormEncodedQueryBody() {
        assertEquals("merId=M810000566&merOrderId=SR202607240001",
                LinkTrustClient.buildQueryBody("M810000566", "SR202607240001"));
    }

    @Test
    @DisplayName("查询请求体：参数值做 URL 编码")
    void queryBodyUrlEncodesValues() {
        assertEquals("merId=M1&merOrderId=A%26B%3DC",
                LinkTrustClient.buildQueryBody("M1", "A&B=C"));
    }

    @Test
    @DisplayName("解析成功响应（官方示例：amount 为字符串）")
    void parsesSuccessResponse() throws Exception {
        String json = """
                {"transactionId":"10170928104830697177","result":"success",
                 "payMethod":"wechatpay","amount":"3000"}
                """;
        LinkTrustTxn txn = LinkTrustClient.parseTxnResponse(json, om);
        assertEquals("10170928104830697177", txn.getTransactionId());
        assertEquals("success", txn.getResult());
        assertEquals("wechatpay", txn.getPayMethod());
        assertEquals(3000L, txn.getAmount());
    }

    @Test
    @DisplayName("解析数值型 amount 与缺失字段")
    void parsesNumericAmountAndMissingFields() throws Exception {
        LinkTrustTxn txn = LinkTrustClient.parseTxnResponse(
                "{\"result\":\"pending\",\"amount\":1500}", om);
        assertEquals("pending", txn.getResult());
        assertEquals(1500L, txn.getAmount());
        assertNull(txn.getTransactionId());
        assertNull(txn.getPayMethod());
    }

    @Test
    @DisplayName("amount 非法或缺失时为 null")
    void invalidAmountBecomesNull() throws Exception {
        assertNull(LinkTrustClient.parseTxnResponse("{\"result\":\"not found\"}", om).getAmount());
        assertNull(LinkTrustClient.parseTxnResponse(
                "{\"result\":\"success\",\"amount\":\"abc\"}", om).getAmount());
    }

    @Test
    @DisplayName("result → 渠道状态映射：success=1, failure/error=2, 其余=0")
    void mapsResultStatus() {
        assertEquals(1, LinkTrustClient.mapResultStatus("success"));
        assertEquals(2, LinkTrustClient.mapResultStatus("failure"));
        assertEquals(2, LinkTrustClient.mapResultStatus("error"));
        assertEquals(0, LinkTrustClient.mapResultStatus("pending"));
        assertEquals(0, LinkTrustClient.mapResultStatus("not found"));
        assertEquals(0, LinkTrustClient.mapResultStatus(null));
    }

    @Test
    @DisplayName("渠道支付方式 → pay_method 编码：wechatpay=1 alipay=2 unionpay=7")
    void mapsPayMethodCode() {
        assertEquals(1, LinkTrustClient.mapPayMethodCode("wechatpay"));
        assertEquals(2, LinkTrustClient.mapPayMethodCode("alipay"));
        assertEquals(7, LinkTrustClient.mapPayMethodCode("unionpay"));
        assertNull(LinkTrustClient.mapPayMethodCode("paypay"));
        assertNull(LinkTrustClient.mapPayMethodCode(null));
    }
}
