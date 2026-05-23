# 商品批量运营与上架流程增强 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为后台商品运营页补上批量上架/下架、批量改分类和批量标签维护，让运营可以一次处理多个商品而不是逐个点开编辑。

**Architecture:** 后端沿用现有 `AdminOperationService -> ProductService -> Repository` 分层，在商品管理能力上新增批量操作接口，并把每次批量修改写入管理员审计日志。前端只在商品列表区增加多选与批量操作条，不重做整体布局，保持当前编辑面板和库存能力可继续共存。

**Tech Stack:** Spring Boot, Spring Data JPA, Vue 3, TypeScript, Vite, JUnit 5, MockMvc

---

### Task 1: 补齐后端批量商品操作测试

**Files:**
- Modify: `backend/src/test/java/com/zyx/jewelry/AdminProductOperationIntegrationTest.java`

- [ ] **Step 1: 写一个失败测试，覆盖批量上架/下架与批量改分类**

```java
@Test
void shouldBulkUpdateProductStatusAndCategory() throws Exception {
    String token = loginAdmin("merch", "Merch@123");
    Long sourceCategoryId = createCategory(token, "Source", "S", 20);
    Long targetCategoryId = createCategory(token, "Target", "T", 21);
    ProductFixture first = createProduct(token, sourceCategoryId, "Batch Ring A", "Batch test", "BATCH-A");
    ProductFixture second = createProduct(token, sourceCategoryId, "Batch Ring B", "Batch test", "BATCH-B");

    mockMvc.perform(post("/api/admin/products/batch")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "productIds": [%d, %d],
                  "status": "OFF_SALE",
                  "categoryId": %d,
                  "tags": ["batch", "featured"]
                }
                """.formatted(first.productId(), second.productId(), targetCategoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updatedCount").value(2));

    mockMvc.perform(get("/api/admin/products/" + first.productId())
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("OFF_SALE"))
        .andExpect(jsonPath("$.data.categoryId").value(targetCategoryId))
        .andExpect(jsonPath("$.data.tags[0]").value("batch"));
}
```

- [ ] **Step 2: 再写一个失败测试，覆盖批量标签维护**

```java
@Test
void shouldBulkAppendTagsToProducts() throws Exception {
    String token = loginAdmin("merch", "Merch@123");
    Long categoryId = createCategory(token, "Tag", "T", 22);
    ProductFixture first = createProduct(token, categoryId, "Tag Ring A", "Batch test", "TAG-A");

    mockMvc.perform(post("/api/admin/products/batch")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "productIds": [%d],
                  "tags": ["new-tag", "promo"]
                }
                """.formatted(first.productId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updatedCount").value(1));

    mockMvc.perform(get("/api/admin/products/" + first.productId())
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tags[0]").value("new-tag"))
        .andExpect(jsonPath("$.data.tags[1]").value("promo"));
}
```

- [ ] **Step 3: 运行这两个测试，确认当前会失败**

Run: `.\mvnw -q -Dtest=AdminProductOperationIntegrationTest#shouldBulkUpdateProductStatusAndCategory,AdminProductOperationIntegrationTest#shouldBulkAppendTagsToProducts test`

Expected: FAIL because `/api/admin/products/batch` does not exist yet.

### Task 2: 实现后端批量商品接口

**Files:**
- Modify: `backend/src/main/java/com/zyx/jewelry/admin/controller/AdminOperationController.java`
- Modify: `backend/src/main/java/com/zyx/jewelry/admin/service/AdminOperationService.java`
- Modify: `backend/src/main/java/com/zyx/jewelry/product/service/ProductService.java`
- Modify: `backend/src/main/java/com/zyx/jewelry/repository/ProductRepository.java`

- [ ] **Step 1: 增加批量请求 DTO 和控制器入口**

```java
@PostMapping("/products/batch")
public ApiResponse<Map<String, Object>> batchUpdateProducts(@Valid @RequestBody BatchUpdateProductRequest request) {
    return ApiResponse.success(adminOperationService.batchUpdateProducts(request));
}

public record BatchUpdateProductRequest(
    @NotNull(message = "商品ID列表不能为空") List<Long> productIds,
    ProductStatus status,
    Long categoryId,
    List<String> tags
) {
}
```

- [ ] **Step 2: 在服务层做权限校验和批量调用**

```java
public Map<String, Object> batchUpdateProducts(AdminOperationController.BatchUpdateProductRequest request) {
    AdminUser adminUser = adminAccessService.requirePermission(AdminPermission.PRODUCT_MANAGE);
    Map<String, Object> result = productService.batchUpdateProducts(request.productIds(), request.status(), request.categoryId(), request.tags());
    adminAuditService.log(adminUser, "PRODUCT_BATCH_UPDATE", "PRODUCT", String.valueOf(request.productIds().size()), "批量更新商品状态/分类/标签");
    return result;
}
```

- [ ] **Step 3: 在商品服务里实现批量状态、分类、标签更新**

```java
@Transactional
public Map<String, Object> batchUpdateProducts(List<Long> productIds, ProductStatus status, Long categoryId, List<String> tags) {
    if (productIds == null || productIds.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "商品ID不能为空");
    }
    List<Product> products = productRepository.findAllById(productIds);
    if (products.size() != productIds.size()) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "存在商品不存在");
    }
    for (Product product : products) {
        if (status != null) {
            product.setStatus(status);
        }
        if (categoryId != null) {
            product.setCategoryId(categoryId);
        }
        if (tags != null) {
            product.setTagsCsv(String.join(",", tags));
        }
    }
    productRepository.saveAll(products);
    return Map.of("updatedCount", products.size());
}
```

- [ ] **Step 4: 运行批量测试，直到通过**

Run: `.\mvnw -q -Dtest=AdminProductOperationIntegrationTest#shouldBulkUpdateProductStatusAndCategory,AdminProductOperationIntegrationTest#shouldBulkAppendTagsToProducts test`

Expected: PASS.

### Task 3: 批量商品选择与操作条

**Files:**
- Modify: `admin/src/api/index.ts`
- Modify: `admin/src/views/AdminDashboard.vue`

- [ ] **Step 1: 写前端失败用例思路并确认接口字段**

```ts
adminApi.batchUpdateProducts({
  productIds: [1, 2],
  status: "OFF_SALE",
  categoryId: 3,
  tags: ["batch", "promo"],
});
```

- [ ] **Step 2: 在商品列表增加多选框与批量操作条**

```vue
<label class="checkbox-row">
  <input type="checkbox" :checked="selectedAll" @change="toggleSelectAll" />
  <span>全选当前列表</span>
</label>
<button type="button" @click="bulkSetStatus('ON_SALE')">批量上架</button>
<button type="button" @click="bulkSetStatus('OFF_SALE')">批量下架</button>
<button type="button" class="secondary-button" @click="bulkMoveCategory">批量改分类</button>
<button type="button" class="secondary-button" @click="bulkEditTags">批量改标签</button>
```

- [ ] **Step 3: 增加前端批量提交逻辑**

```ts
await adminApi.batchUpdateProducts({
  productIds: selectedProductIds.value,
  status,
});
await loadProducts();
selectedProductIds.value = [];
```

- [ ] **Step 4: 保证现有单品编辑、库存、流水不受影响**

### Task 4: 验证与提交

**Files:**
- Modify: `admin/src/api/index.ts`
- Modify: `admin/src/views/AdminDashboard.vue`
- Modify: 后端新增接口相关文件

- [ ] **Step 1: 跑后端全量测试**

Run: `.\mvnw -q test`

- [ ] **Step 2: 跑前端构建**

Run: `npm run build`

- [ ] **Step 3: 查看 Git 状态并提交**

Run:
```powershell
git status --short --branch
git add .
git commit -m "feat: add bulk product operations"
```

- [ ] **Step 4: 推送到远程**

Run: `git push origin main`

