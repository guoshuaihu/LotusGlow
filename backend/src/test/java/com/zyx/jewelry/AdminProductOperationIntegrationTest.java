package com.zyx.jewelry;

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
class AdminProductOperationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSearchFilterGetDetailAndEditProduct() throws Exception {
        String token = loginAdmin("merch", "Merch@123");
        Long categoryId = createCategory(token, "Bracelet", "B", 9);
        Long anotherCategoryId = createCategory(token, "Necklace", "N", 8);

        ProductFixture product = createProduct(token, categoryId, "Aurora Bracelet", "Layered gold bracelet", "BR-001");
        Long productId = product.productId();
        createProduct(token, anotherCategoryId, "Starlight Necklace", "Luxury necklace", "NK-001");

        mockMvc.perform(get("/api/admin/products")
                .header("Authorization", bearer(token))
                .param("keyword", "Aurora")
                .param("categoryId", String.valueOf(categoryId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("Aurora Bracelet"))
            .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/admin/products/" + productId)
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Aurora Bracelet"))
            .andExpect(jsonPath("$.data.skus.length()").value(1));

        MvcResult editedResult = mockMvc.perform(put("/api/admin/products/" + productId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "Aurora Bracelet Pro",
                      "subtitle": "Updated subtitle",
                      "productNo": "BR-001-PRO",
                      "basePrice": 2999,
                      "description": "Updated product description",
                      "certificateInfo": "GIC Certified",
                      "serviceInfo": "Gift box and express shipping",
                      "supportCustom": true,
                      "hotFlag": true,
                      "newFlag": false,
                      "tags": ["gift", "pro"],
                      "status": "ON_SALE",
                      "media": [
                        {"id": null, "mediaType": "IMAGE", "mediaUrl": "https://example.com/updated.jpg", "sortOrder": 1}
                      ],
                      "skus": [
                        {"id": %d, "skuCode": "BR-001-G", "material": "18K Gold", "ringSize": "M", "weightDesc": "3.2g", "salePrice": 2999, "stock": 12, "status": "ENABLED"},
                        {"id": null, "skuCode": "BR-001-W", "material": "18K White Gold", "ringSize": "L", "weightDesc": "3.4g", "salePrice": 3199, "stock": 8, "status": "ENABLED"}
                      ]
                    }
                    """.formatted(categoryId, product.skuId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Aurora Bracelet Pro"))
            .andExpect(jsonPath("$.data.skus.length()").value(2))
            .andReturn();

        Long updatedSkuId = readId(editedResult, "data.skus.0.id");

        mockMvc.perform(put("/api/admin/products/" + productId + "/stock")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId": %d,
                      "newStock": 15,
                      "reason": "manual replenishment"
                    }
                    """.formatted(updatedSkuId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stock").value(15));
    }

    @Test
    void shouldRejectProductManagementForCustomerService() throws Exception {
        String token = loginAdmin("service", "Service@123");

        mockMvc.perform(get("/api/admin/products")
                .header("Authorization", bearer(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldListLowStockProductsAndInventoryRecords() throws Exception {
        String token = loginAdmin("merch", "Merch@123");
        Long categoryId = createCategory(token, "Inventory", "I", 10);
        ProductFixture product = createProduct(token, categoryId, "Low Stock Ring", "Needs replenishment", "LOW-001");

        mockMvc.perform(put("/api/admin/products/" + product.productId() + "/stock")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId": %d,
                      "newStock": 2,
                      "reason": "stock count correction"
                    }
                    """.formatted(product.skuId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lowStock").value(true))
            .andExpect(jsonPath("$.data.lowStockThreshold").value(5));

        mockMvc.perform(get("/api/admin/products")
                .header("Authorization", bearer(token))
                .param("lowStockOnly", "true")
                .param("stockThreshold", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("Low Stock Ring"))
            .andExpect(jsonPath("$.data[0].lowStock").value(true))
            .andExpect(jsonPath("$.data[0].lowStockSkuCount").value(1))
            .andExpect(jsonPath("$.data[0].skus[0].lowStock").value(true));

        mockMvc.perform(get("/api/admin/products/" + product.productId() + "/inventory-records")
                .header("Authorization", bearer(token))
                .param("skuId", String.valueOf(product.skuId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].skuId").value(product.skuId()))
            .andExpect(jsonPath("$.data[0].beforeStock").value(6))
            .andExpect(jsonPath("$.data[0].afterStock").value(2))
            .andExpect(jsonPath("$.data[0].changeReason").value("stock count correction"));
    }

    private Long createCategory(String token, String name, String icon, Integer sortOrder) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "icon": "%s",
                      "sortOrder": %d
                    }
                    """.formatted(name, icon, sortOrder)))
            .andExpect(status().isOk())
            .andReturn();
        return readId(result, "data.id");
    }

    private ProductFixture createProduct(String token, Long categoryId, String name, String subtitle, String productNo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/products")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "%s",
                      "subtitle": "%s",
                      "productNo": "%s",
                      "basePrice": 2599,
                      "description": "Fine jewelry product",
                      "certificateInfo": "GIC Certified",
                      "serviceInfo": "Express shipping and gift box",
                      "supportCustom": true,
                      "hotFlag": false,
                      "newFlag": true,
                      "tags": ["gift", "new"],
                      "status": "ON_SALE",
                      "media": [
                        {"mediaType": "IMAGE", "mediaUrl": "https://example.com/item.jpg", "sortOrder": 1}
                      ],
                      "skus": [
                        {"skuCode": "%s-SKU", "material": "18K Gold", "ringSize": "M", "weightDesc": "3g", "salePrice": 2599, "stock": 6, "status": "ENABLED"}
                      ]
                    }
                    """.formatted(categoryId, name, subtitle, productNo, productNo)))
            .andExpect(status().isOk())
            .andReturn();
        return new ProductFixture(readId(result, "data.id"), readId(result, "data.skus.0.id"));
    }

    private record ProductFixture(Long productId, Long skuId) {
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

    private String loginAdmin(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn();
        return readText(result, "data.token");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
