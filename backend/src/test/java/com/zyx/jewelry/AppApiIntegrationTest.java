package com.zyx.jewelry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AppApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLoadHomePageAndFilterProducts() throws Exception {
        mockMvc.perform(get("/api/app/home"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.banners[0].title").value("鎏光臻礼季"))
            .andExpect(jsonPath("$.data.hotProducts[0].name").value("星河密镶钻戒"));

        mockMvc.perform(get("/api/app/products")
                .param("keyword", "星河")
                .param("categoryId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].name").value("星河密镶钻戒"));
    }

    @Test
    void shouldSupportCartOrderFavoriteAndCustomRequestFlow() throws Exception {
        String token = loginAppUser();

        MvcResult addressResult = mockMvc.perform(post("/api/app/addresses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "receiverName":"林小溪",
                      "receiverPhone":"13800138000",
                      "province":"上海市",
                      "city":"上海市",
                      "district":"浦东新区",
                      "detailAddress":"世纪大道 100 号",
                      "isDefault":true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.receiverName").value("林小溪"))
            .andReturn();

        Long addressId = readId(addressResult, "data.id");

        MvcResult cartResult = mockMvc.perform(post("/api/app/cart/items")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId":1,
                      "quantity":1,
                      "engravingText":"FOREVER",
                      "sizeRemark":"12号圈",
                      "materialRemark":"18K金"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.productName").value("星河密镶钻戒"))
            .andReturn();

        Long cartItemId = readId(cartResult, "data.id");

        mockMvc.perform(get("/api/app/cart")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].quantity").value(1));

        mockMvc.perform(post("/api/app/favorites/1")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorited").value(true));

        mockMvc.perform(post("/api/app/custom-requests")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId":1,
                      "contactName":"林小溪",
                      "contactPhone":"13800138000",
                      "engravingText":"FOREVER",
                      "sizeRemark":"12号圈",
                      "materialRemark":"18K金",
                      "remark":"希望在纪念日前送达"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING_FOLLOW_UP"));

        MvcResult orderResult = mockMvc.perform(post("/api/app/orders")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "addressId":%d,
                      "cartItemIds":[%d],
                      "buyerRemark":"请附带礼盒包装"
                    }
                    """.formatted(addressId, cartItemId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("WAITING_PAYMENT"))
            .andExpect(jsonPath("$.data.items[0].quantity").value(1))
            .andReturn();

        String orderNo = readText(orderResult, "data.orderNo");

        MvcResult payResult = mockMvc.perform(post("/api/app/orders/" + orderNo + "/pay")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderNo").value(orderNo))
            .andReturn();

        String paymentNo = readText(payResult, "data.paymentNo");

        String notifyBody = """
            {
              "paymentNo":"%s",
              "transactionId":"WX202605120001",
              "paidAmount":6999.00
            }
            """.formatted(paymentNo);

        mockMvc.perform(post("/api/payments/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(notifyBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("WAITING_SHIPMENT"));

        mockMvc.perform(post("/api/payments/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(notifyBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("WAITING_SHIPMENT"));

        mockMvc.perform(get("/api/app/orders")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("WAITING_SHIPMENT"));

        mockMvc.perform(delete("/api/app/cart/items/" + cartItemId)
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.removed").value(false));
    }

    @Test
    void shouldAllowAdminShipOrder() throws Exception {
        String appToken = loginAppUser();
        Long addressId = createAddress(appToken);
        Long cartItemId = createCartItem(appToken);
        String orderNo = createOrder(appToken, addressId, cartItemId);
        String paymentNo = createPayment(appToken, orderNo);

        mockMvc.perform(post("/api/payments/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "paymentNo":"%s",
                      "transactionId":"WX202605120088",
                      "paidAmount":6999.00
                    }
                    """.formatted(paymentNo)))
            .andExpect(status().isOk());

        MvcResult adminLogin = mockMvc.perform(post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"admin",
                      "password":"Admin@123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("admin"))
            .andReturn();

        String adminToken = readText(adminLogin, "data.token");

        mockMvc.perform(post("/api/admin/orders/" + orderNo + "/ship")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "company":"顺丰速运",
                      "trackingNo":"SF202605120001"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(get("/api/app/orders/" + orderNo)
                .header("Authorization", bearer(appToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.logisticsCompany").value("顺丰速运"))
            .andExpect(jsonPath("$.data.status").value("SHIPPED"));
    }

    @Test
    void shouldRejectUnavailableSkuOrder() throws Exception {
        String token = loginAppUser();
        Long addressId = createAddress(token);

        mockMvc.perform(post("/api/app/orders/direct")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "addressId":%d,
                      "skuId":999,
                      "quantity":1
                    }
                    """.formatted(addressId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("商品规格不存在或已下架"));
    }

    private String loginAppUser() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/app/auth/wx-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code":"wx-demo-user",
                      "nickname":"林小溪"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.nickname").value("林小溪"))
            .andReturn();
        return readText(loginResult, "data.token");
    }

    private Long createAddress(String token) throws Exception {
        return readId(mockMvc.perform(post("/api/app/addresses")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "receiverName":"林小溪",
                      "receiverPhone":"13800138000",
                      "province":"上海市",
                      "city":"上海市",
                      "district":"上海市",
                      "detailAddress":"陆家嘴金融中心 8 楼",
                      "isDefault":true
                    }
                    """))
            .andReturn(), "data.id");
    }

    private Long createCartItem(String token) throws Exception {
        return readId(mockMvc.perform(post("/api/app/cart/items")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId":1,
                      "quantity":1
                    }
                    """))
            .andReturn(), "data.id");
    }

    private String createOrder(String token, Long addressId, Long cartItemId) throws Exception {
        return readText(mockMvc.perform(post("/api/app/orders")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "addressId":%d,
                      "cartItemIds":[%d]
                    }
                    """.formatted(addressId, cartItemId)))
            .andReturn(), "data.orderNo");
    }

    private String createPayment(String token, String orderNo) throws Exception {
        return readText(mockMvc.perform(post("/api/app/orders/" + orderNo + "/pay")
                .header("Authorization", bearer(token)))
            .andReturn(), "data.paymentNo");
    }

    private Long readId(MvcResult result, String path) throws Exception {
        return Long.valueOf(readText(result, path));
    }

    private String readText(MvcResult result, String path) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String part : path.split("\\.")) {
            node = node.get(part);
        }
        return node.asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
