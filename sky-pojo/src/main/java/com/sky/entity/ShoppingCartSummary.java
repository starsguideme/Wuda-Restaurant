package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ShoppingCartSummary {
    private Integer totalCount;           // 总件数（如 8 件）
    private BigDecimal totalAmount;       // 总金额（如 ¥256.00）
    private List<CartPreview> previews;   // 最近添加的 3~5 件商品
    private Long version;                 // 版本号，用于乐观锁/缓存更新

    @Data
    @AllArgsConstructor
    public static class CartPreview {
        private String name;      // 商品名
        private String image;     // 图片
        private Integer number;   // 数量
        private BigDecimal price; // 单价
    }
}