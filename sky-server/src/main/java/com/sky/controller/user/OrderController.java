package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.UserOrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags="用户端订单接口")
@Slf4j
public class OrderController {
    @Autowired
    private UserOrderService userOrderService;
    
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO)
    {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = userOrderService.submit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }
    
    // TODO: 【分布式锁】防止用户重复提交订单
    // 练习目标：使用 Redis SETNX 实现分布式锁，防止同一用户同时多次下单
    // 提示：lock:order:submit:{userId}，设置 5 秒过期时间
    // private boolean tryLock(String userId) {
    //     return Boolean.TRUE.equals(redisTemplate.opsForValue()
    //         .setIfAbsent("lock:order:submit:" + userId, "1", 5, TimeUnit.SECONDS));
    // }
    // private void unlock(String userId) {
    //     redisTemplate.delete("lock:order:submit:" + userId);
    // }
    
    /**F
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = userOrderService.payment(ordersPaymentDTO);
        return Result.success(orderPaymentVO);
    }
    
    // TODO: 【幂等性】支付回调防重复处理
    // 练习目标：使用 Redis 记录已处理的订单号，防止微信重复回调
    // 提示：payment:processed:{orderNumber}，设置 24 小时过期
    // private boolean isPaymentProcessed(String orderNumber) {
    //     return Boolean.TRUE.equals(redisTemplate.hasKey("payment:processed:" + orderNumber));
    // }
    // private void markPaymentProcessed(String orderNumber) {
    //     redisTemplate.opsForValue().set("payment:processed:" + orderNumber, "1", 24, TimeUnit.HOURS);
    // }
    
    @GetMapping("/historyOrders")
    @ApiOperation("查看历史订单")
    public Result<PageResult> historyOrders(Integer page, Integer pageSize,Integer  status)
    {
        log.info("查看历史订单：page={}, pageSize={}", page, pageSize);
        PageResult pageResult = userOrderService.pageQuery4User(page, pageSize,status);
        return Result.success(pageResult);
    }
    
    // TODO: 【接口限流】防止恶意刷单
    // 练习目标：限制同一用户 1 分钟内最多提交 5 次订单
    // 提示：使用滑动窗口算法，key: rate_limit:order:{userId}
    // private boolean isRateLimited(String userId) {
    //     String key = "rate_limit:order:" + userId;
    //     Long count = redisTemplate.opsForValue().increment(key);
    //     if (count == 1) {
    //         redisTemplate.expire(key, 60, TimeUnit.SECONDS);
    //     }
    //     return count > 5;
    // }
    
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("订单详情")
    public Result<OrderVO> orderDetail(@PathVariable("id") Long id)
    {
        log.info("订单详情，订单id：{}", id);
        OrderVO orderVO = userOrderService.getOrderDetail(id);
        return Result.success(orderVO);
    }
    @GetMapping("/reminder/{id}")
    @ApiOperation("订单催单")
    public Result reminder(@PathVariable("id") Long id)
    {
        log.info("订单催单，订单id：{}", id);
        userOrderService.reminder(id);
        return Result.success();
    }
    
    // TODO: 【乐观锁】更新订单状态防并发冲突
    // 练习目标：使用 version 字段实现乐观锁，防止同时修改订单状态
    // 提示：UPDATE orders SET status=?, version=version+1 WHERE id=? AND version=?
    // private boolean updateOrderWithOptimisticLock(Long orderId, Integer status, Integer version) {
    //     return userOrderMapper.updateWithVersion(orderId, status, version) > 0;
    // }
    
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable("id") Long id) throws Exception {
        log.info("取消订单，订单id：{}", id);
        userOrderService.cancel(id);
        return Result.success();
    }
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable("id") Long id)
    {
        log.info("再来一单，订单id：{}", id);
        userOrderService.repetition(id);
        return Result.success();
    }
}