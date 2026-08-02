package com.sky.service;

public interface InventoryService {
    /**
     * 原子扣减库存，成功返回 true，失败返回 false
     */
    boolean decrement(Long productId, int amount);
}
