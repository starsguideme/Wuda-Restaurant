package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 简单库存扣减接口（使用 Redis Lua 原子扣减）
 */
@RestController
@RequestMapping("/user/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/decrement")
    public Result<Boolean> decrement(@RequestBody Map<String, Object> body){
        Long productId = Long.valueOf(body.get("productId").toString());
        Integer amount = Integer.valueOf(body.get("amount").toString());
        boolean ok = inventoryService.decrement(productId, amount);
        if(ok) return Result.success(true);
        return Result.error("库存不足或扣减失败");
    }
}
