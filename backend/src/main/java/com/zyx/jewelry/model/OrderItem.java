package com.zyx.jewelry.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuSummary;

    private String coverImage;

    private BigDecimal salePrice;

    private Integer quantity;

    private BigDecimal subtotalAmount;

    private String engravingText;

    private String sizeRemark;

    private String materialRemark;
}
