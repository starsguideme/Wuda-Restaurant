package com.sky.service.impl;

import com.sky.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Lua 脚本：如果库存 >= 要求的数量则扣减并返回 1，否则返回 0
    private static final String DECR_LUA =
            "local stock = redis.call('get', KEYS[1])\n" +
            "if (not stock) then return -1 end\n" +
            "stock = tonumber(stock)\n" +
            "local num = tonumber(ARGV[1])\n" +
            "if (stock >= num) then\n" +
            "  redis.call('decrby', KEYS[1], num)\n" +
            "  return 1\n" +
            "else\n" +
            "  return 0\n" +
            "end";

    @Override
    public boolean decrement(Long productId, int amount) {
        String key = "stock:" + productId;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(DECR_LUA);
        script.setResultType(Long.class);
        Long res = stringRedisTemplate.execute(script, Collections.singletonList(key), String.valueOf(amount));
        if(res == null) return false;
        if(res == 1L) return true;
        return false;
    }
}
