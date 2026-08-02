package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;
    private final static String KEY = "SHOP_STATUS";
    @PutMapping("/{status}")
    @ApiOperation("设置营业状态")
    public Result setStatus(@PathVariable Integer status)
    {
        log.info("设置店铺营业状态为{}" ,status==1?"营业中":"打烊中");
        //存储到Redis中然后转化成String类型的格式
        redisTemplate.opsForValue().set(KEY,status.toString());
        return Result.success();
    }
    @GetMapping("/status")
    @ApiOperation("获取营业状态")
    public Result<Integer> getStatus()
    {
        //如果一定要使用String类型的值，就必须先创建一个String类型的中间变量
        String statusStr= (String) redisTemplate.opsForValue().get(KEY);
        //然后再将中间变量转为int类型（需要使用Integer包装器中int转换方法才行）
        Integer status = Integer.parseInt(statusStr);
        log.info("获取到店铺的营业状态为：{}",status == '1' ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
