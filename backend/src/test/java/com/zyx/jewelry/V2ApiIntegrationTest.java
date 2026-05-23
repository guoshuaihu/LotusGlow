package com.zyx.jewelry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class V2ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSupportPhoneBindingTokenRefreshAndAddressCrud() throws Exception {
        String userToken = loginAppUser("wx-v2-user");

        mockMvc.perform(post("/api/app/auth/bind-phone")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "phone":"13900000001"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.phone").value("13900000001"));

        mockMvc.perform(post("/api/app/auth/refresh")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.user.phone").value("13900000001"));

        MvcResult createdAddress = mockMvc.perform(post("/api/app/addresses")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "receiverName":"Alice",
                      "receiverPhone":"13900000001",
                      "province":"Shanghai",
                      "city":"Shanghai",
                      "district":"Pudong",
                      "detailAddress":"No.1 Century Avenue",
                      "isDefault":true
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        Long addressId = readId(createdAddress, "data.id");

        mockMvc.perform(put("/api/app/addresses/" + addressId)
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "receiverName":"Alice Chen",
                      "receiverPhone":"13900000002",
                      "province":"Shanghai",
                      "city":"Shanghai",
                      "district":"Minhang",
                      "detailAddress":"No.88 Luxury Road",
                      "isDefault":true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiverName").value("Alice Chen"));

        mockMvc.perform(get("/api/app/addresses")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].district").value("Minhang"));

        mockMvc.perform(delete("/api/app/addresses/" + addressId)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.removed").value(true));
    }

    @Test
    void shouldEnforceRbacAndSupportProductOperations() throws Exception {
        String productAdminToken = loginAdmin("merch", "Merch@123");
        String customerServiceToken = loginAdmin("service", "Service@123");

        MvcResult categoryResult = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", bearer(productAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Bracelet",
                      "icon":"B",
                      "sortOrder":9
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Bracelet"))
            .andReturn();

        Long categoryId = readId(categoryResult, "data.id");

        MvcResult productResult = mockMvc.perform(post("/api/admin/products")
                .header("Authorization", bearer(productAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId":%d,
                      "name":"Aurora Bracelet",
                      "subtitle":"Layered gold bracelet",
                      "productNo":"BRACELET-001",
                      "basePrice":2599,
                      "description":"Fine jewelry bracelet for gifting",
                      "certificateInfo":"GIC Certified",
                      "serviceInfo":"Express shipping and gift box",
                      "supportCustom":true,
                      "hotFlag":false,
                      "newFlag":true,
                      "tags":["gift","new"],
                      "status":"ON_SALE",
                      "media":[
                        {"mediaType":"IMAGE","mediaUrl":"https://example.com/b1.jpg","sortOrder":1}
                      ],
                      "skus":[
                        {"skuCode":"BR-001-G","material":"18K Gold","ringSize":"M","weightDesc":"3g","salePrice":2599,"stock":6,"status":"ENABLED"}
                      ]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Aurora Bracelet"))
            .andExpect(jsonPath("$.data.skus[0].stock").value(6))
            .andReturn();

        Long productId = readId(productResult, "data.id");
        Long skuId = readId(productResult, "data.skus.0.id");

        mockMvc.perform(put("/api/admin/products/" + productId + "/stock")
                .header("Authorization", bearer(productAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId":%d,
                      "newStock":10,
                      "reason":"manual replenishment"
                    }
                    """.formatted(skuId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stock").value(10));

        mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", bearer(customerServiceToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Forbidden",
                      "icon":"X",
                      "sortOrder":99
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldSupportAfterSaleAuditAndRefundFlow() throws Exception {
        String userToken = loginAppUser("wx-after-sale-user");
        Long addressId = createAddress(userToken);
        Long cartItemId = createCartItem(userToken);
        String orderNo = createOrder(userToken, addressId, cartItemId);
        String paymentNo = createPayment(userToken, orderNo);

        mockMvc.perform(post("/api/payments/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "paymentNo":"%s",
                      "transactionId":"WX-V2-0001",
                      "paidAmount":6999.00
                    }
                    """.formatted(paymentNo)))
            .andExpect(status().isOk());

        String serviceToken = loginAdmin("service", "Service@123");

        mockMvc.perform(post("/api/admin/orders/" + orderNo + "/ship")
                .header("Authorization", bearer(serviceToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "company":"SF",
                      "trackingNo":"SF-10001"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/app/orders/" + orderNo + "/confirm")
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        Long orderItemId = readId(mockMvc.perform(get("/api/app/orders/" + orderNo)
                .header("Authorization", bearer(userToken)))
            .andReturn(), "data.items.0.id");

        MvcResult afterSaleResult = mockMvc.perform(post("/api/app/after-sales")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "orderNo":"%s",
                      "orderItemId":%d,
                      "reason":"quality_issue",
                      "description":"stone loose after delivery"
                    }
                    """.formatted(orderNo, orderItemId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn();

        Long afterSaleId = readId(afterSaleResult, "data.id");

        mockMvc.perform(post("/api/admin/after-sales/" + afterSaleId + "/audit")
                .header("Authorization", bearer(serviceToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "approved":true,
                      "remark":"approved for refund"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        mockMvc.perform(get("/api/admin/after-sales")
                .header("Authorization", bearer(serviceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].refundStatus").value("SUCCESS"));

        mockMvc.perform(get("/api/app/orders/" + orderNo)
                .header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }

    private String loginAppUser(String code) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/app/auth/wx-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code":"%s",
                      "nickname":"V2 User"
                    }
                    """.formatted(code)))
            .andExpect(status().isOk())
            .andReturn();
        return readText(loginResult, "data.token");
    }

    private String loginAdmin(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"%s"
                    }
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn();
        return readText(result, "data.token");
    }

    private Long createAddress(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/app/addresses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "receiverName":"Bob",
                      "receiverPhone":"13900000003",
                      "province":"Shanghai",
                      "city":"Shanghai",
                      "district":"Pudong",
                      "detailAddress":"No.9 River Road",
                      "isDefault":true
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        return readId(result, "data.id");
    }

    private Long createCartItem(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/app/cart/items")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId":1,
                      "quantity":1
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        return readId(result, "data.id");
    }

    private String createOrder(String token, Long addressId, Long cartItemId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/app/orders")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "addressId":%d,
                      "cartItemIds":[%d],
                      "buyerRemark":"v2 order"
                    }
                    """.formatted(addressId, cartItemId)))
            .andExpect(status().isOk())
            .andReturn();
        return readText(result, "data.orderNo");
    }

    private String createPayment(String token, String orderNo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/app/orders/" + orderNo + "/pay")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
        return readText(result, "data.paymentNo");
    }

    private Long readId(MvcResult result, String path) throws Exception {
        return Long.valueOf(readText(result, path));
    }

    private String readText(MvcResult result, String path) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String part : path.split("\\.")) {
            if (part.matches("\\d+")) {
                node = node.get(Integer.parseInt(part));
            } else {
                node = node.get(part);
            }
        }
        return node.asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
