package com.sky.entity;

import ch.qos.logback.classic.db.names.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
//@TableName("shopping_cart_item")   // 明确指定表名
public class ShoppingCartItem {
    private Long id;
    private Long userId;
    private Long dishId;
    private Long setmealId;
    private String name;
    private String image;
    private BigDecimal price;
    private Integer number;
    private String flavor;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}