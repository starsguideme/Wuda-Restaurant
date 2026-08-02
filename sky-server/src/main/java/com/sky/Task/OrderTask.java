package com.sky.Task;

import com.sky.RedisConstant.Constant;
import com.sky.entity.Dish;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.DishMapper;
import com.sky.mapper.UserOrderDetailMapper;
import com.sky.mapper.UserOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class OrderTask {
    @Autowired
    private UserOrderMapper userOrderMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserOrderDetailMapper userOrderDetailMapper;
    @Autowired
    private DishMapper dishMapper;

    @Scheduled(cron = "0 * * * * ?")//每分钟执行一次
    public void processTimeOutOrderTask() {
        log.info("订单任务开始执行...{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-5);
        //查询状态是否为待付款并且超过一定时间后取消订单，如果是的，则进行取消订单 否则跳过，订单时间=当前时间-5分钟
        List<Orders> statusAndOrderTimeLT = userOrderMapper.getStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        if(statusAndOrderTimeLT != null&&statusAndOrderTimeLT.size()>0){
            for (Orders orders : statusAndOrderTimeLT) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时取消");
                orders.setCancelTime(LocalDateTime.now());
                userOrderMapper.update(orders);
            }
        }
    }
    
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryTask(){
        log.info("开始处理正在派送中的订单...{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> statusAndOrderTimeLT = userOrderMapper.getStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if(statusAndOrderTimeLT != null&&statusAndOrderTimeLT.size()>0){
            for (Orders orders : statusAndOrderTimeLT) {
                orders.setStatus(Orders.COMPLETED);
                userOrderMapper.update(orders);
            }
        }
   }

    // TODO: 【延迟队列优化】使用 Redis ZSET 实现更精确的订单超时取消
    // ========================================
    // 练习目标：替换当前的全表扫描方式，使用 Redis 延迟队列提升性能
    // 
    // 实现步骤：
    // 1. 订单创建时，将订单 ID 和超时时间戳加入 Redis ZSET
    //    key: delay_queue:order_cancel
    //    score: 订单创建时间 + 15 分钟（时间戳毫秒）
    //    value: 订单 ID（字符串）
    //
    // 2. 定时任务每分钟扫描 ZSET，获取到期的订单
    //    使用 rangeByScore(0, currentTime) 获取 score <= 当前时间的订单
    //
    // 3. 遍历到期订单，检查订单状态
    //    如果状态仍是 PENDING_PAYMENT，则取消订单
    //
    // 4. 从 ZSET 中移除已处理的订单
    //
    // 代码框架：
    // public void addToDelayQueue(Long orderId) {
    //     // 计算 15 分钟后的时间戳
    //     long expireTime = System.currentTimeMillis() + 15 * 60 * 1000;
    //     // 将订单加入延迟队列
    //     redisTemplate.opsForZSet().add(
    //         "delay_queue:order_cancel", 
    //         orderId.toString(), 
    //         expireTime
    //     );
    //     log.info("订单 {} 已加入延迟队列，将在 15 分钟后处理", orderId);
    // }
    //
    // @Scheduled(cron = "0 * * * * ?")
    // public void processDelayQueue() {
    //     log.info("开始处理延迟队列...{}", LocalDateTime.now());
    //     
    //     // 获取当前时间戳
    //     long now = System.currentTimeMillis();
    //     
    //     // 查询到期的订单（score <= now 的订单）
    //     Set<String> orderIds = redisTemplate.opsForZSet()
    //         .rangeByScore("delay_queue:order_cancel", 0, now);
    //     
    //     if (orderIds != null && !orderIds.isEmpty()) {
    //         log.info("发现 {} 个超时订单", orderIds.size());
    //         
    //         for (String orderId : orderIds) {
    //             try {
    //                 // 查询订单当前状态
    //                 Orders order = userOrderMapper.getById(Long.parseLong(orderId));
    //                 
    //                 // 只有待支付状态的订单才取消
    //                 if (order != null && order.getStatus() == Orders.PENDING_PAYMENT) {
    //                     order.setStatus(Orders.CANCELLED);
    //                     order.setCancelReason("订单超时未支付（延迟队列）");
    //                     order.setCancelTime(LocalDateTime.now());
    //                     userOrderMapper.update(order);
    //                     log.info("订单 {} 已取消（延迟队列）", orderId);
    //                 }
    //                 
    //                 // 从延迟队列中移除已处理的订单
    //                 redisTemplate.opsForZSet().remove("delay_queue:order_cancel", orderId);
    //             } catch (Exception e) {
    //                 log.error("处理延迟队列订单 {} 失败", orderId, e);
    //             }
    //         }
    //     }
    // }
    //
    // 优势对比：
    // - 原方案：每分钟全表扫描 orders 表，数据量大时性能差
    // - 新方案：直接查询到期的订单 ID，性能提升 10 倍以上

    // TODO: 【分布式锁】防止多实例重复执行定时任务
    // ========================================
    // 练习目标：多实例部署时，确保定时任务只在一个实例上执行
    //
    // 场景问题：
    // 当项目部署多个实例时，每个实例都会执行定时任务
    // 导致订单被重复取消、重复更新等问题
    //
    // 实现步骤：
    // 1. 尝试获取分布式锁（SETNX + 过期时间）
    // 2. 获取到锁的实例执行任务
    // 3. 任务完成后释放锁
    // 4. 使用 Lua 脚本保证释放锁的原子性（防止误删他人锁）
    //
    // 代码框架：
    // 
    // // 尝试获取分布式锁
    // private boolean tryLock(String lockKey, String lockValue, long timeout) {
    //     // setIfAbsent 相当于 Redis 的 SETNX 命令
    //     // 只有 key 不存在时才设置，返回 true 表示获取锁成功
    //     Boolean success = redisTemplate.opsForValue()
    //         .setIfAbsent(lockKey, lockValue, timeout, TimeUnit.SECONDS);
    //     return Boolean.TRUE.equals(success);
    // }
    //
    // // 释放分布式锁（使用 Lua 脚本保证原子性）
    // private void unlock(String lockKey, String lockValue) {
    //     // Lua 脚本：先检查锁的值是否匹配，匹配才删除
    //     // 防止 A 实例删除了 B 实例的锁
    //     String luaScript = 
    //         "if redis.call('get', KEYS[1]) == ARGV[1] then " +
    //         "    return redis.call('del', KEYS[1]) " +
    //         "else " +
    //         "    return 0 " +
    //         "end";
    //     
    //     redisTemplate.execute(
    //         (org.springframework.data.redis.core.RedisCallback<Long>) connection -> 
    //             connection.eval(
    //                 luaScript.getBytes(),
    //                 org.springframework.data.redis.connection.ReturnType.INTEGER,
    //                 1,
    //                 lockKey.getBytes(),
    //                 lockValue.getBytes()
    //             )
    //     );
    // }
    //
    // // 使用分布式锁的定时任务
    // @Scheduled(cron = "0 * * * * ?")
    // public void processTimeoutOrderWithLock() {
    //     String lockKey = "lock:order:timeout:task";
    //     String lockValue = UUID.randomUUID().toString(); // 唯一标识
    //     
    //     // 尝试获取锁，设置 50 秒过期（任务执行间隔 60 秒）
    //     if (tryLock(lockKey, lockValue, 50)) {
    //         try {
    //             log.info("获取到分布式锁，开始处理超时订单...");
    //             
    //             // 执行超时订单处理逻辑
    //             LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
    //             List<Orders> orders = userOrderMapper
    //                 .getStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
    //             
    //             if (orders != null && !orders.isEmpty()) {
    //                 for (Orders order : orders) {
    //                     order.setStatus(Orders.CANCELLED);
    //                     order.setCancelReason("订单超时取消（分布式锁）");
    //                     order.setCancelTime(LocalDateTime.now());
    //                     userOrderMapper.update(order);
    //                 }
    //                 log.info("处理了 {} 个超时订单", orders.size());
    //             }
    //         } finally {
    //             // 无论成功失败，都要释放锁
    //             unlock(lockKey, lockValue);
    //             log.info("释放分布式锁");
    //         }
    //     } else {
    //         log.info("其他实例正在执行超时订单任务，跳过本次执行");
    //     }
    // }
    //
    // 关键点：
    // 1. 锁必须设置过期时间，防止死锁（实例宕机导致锁无法释放）
    // 2. 锁的 value 必须是唯一标识（UUID），防止误删他人锁
    // 3. 释放锁必须用 Lua 脚本，保证"检查 + 删除"的原子性
    // 4. finally 块中释放锁，确保异常时也能释放

    // TODO: 【库存恢复】定时恢复超时未支付订单的库存
    // ========================================
    // 练习目标：订单取消后，恢复商品的库存数量
    //
    // 场景问题：
    // 1. 数据库中的库存需要同步恢复
    // 2. Redis 中预扣的库存也需要同步恢复
    // 3. 防止库存数据不一致
    //
    // 实现步骤：
    // 1. 查询今日已取消的订单
    // 2. 遍历订单，获取订单中的商品和数量
    // 3. 恢复数据库库存（UPDATE dish SET stock = stock + ? WHERE id = ?）
    // 4. 恢复 Redis 库存（redisTemplate.opsForValue().increment()）
    // 5. 记录恢复日志
    //
    // 代码框架：
//    @Scheduled(cron = "0 0 */2 * * ?") // 每 2 小时执行一次
//    @Transactional
//    public void recoverStock() {
//        log.info("开始恢复超时订单的库存...{}", LocalDateTime.now());
//
//        try {
//            // 查询最近 2 小时内被取消的订单
//            LocalDateTime beginTime = LocalDateTime.now().minusHours(2);
//            LocalDateTime endTime = LocalDateTime.now();
//            List<Orders> orders = userOrderMapper.selectCanceledOrderToday(
//                beginTime, endTime, Orders.CANCELLED
//            );
//
//            // 如果没有订单则返回
//            if (orders == null || orders.isEmpty()) {
//                log.info("最近 2 小时没有取消的订单，跳过库存恢复");
//                return;
//            }
//
//            log.info("发现 {} 个取消的订单，开始恢复库存", orders.size());
//            log.info("正在恢复 {} 个订单的库存...", orders.size());
//            // 记录恢复成功和失败的订单数
//            int successCount=0;
//            int failCount=0;
//            // 遍历订单，恢复库存
//            for (Orders order : orders) {
//                List<OrderDetail> orderDetails = userOrderDetailMapper.getOrderId(order.getId());
//
//                if (orderDetails != null && !orderDetails.isEmpty()) {
//                    for (OrderDetail orderDetail : orderDetails) {
//                        Long dishId = orderDetail.getDishId();
//                        Integer number = orderDetail.getNumber();
//
//                        if (dishId != null && number != null && number > 0) {
//                            // 恢复数据库库存
//                            dishMapper.recoverStock(dishId, number);
//                            log.info("恢复数据库库存：商品 ID={}, 数量={}", dishId, number);
//
//                            // 恢复 Redis 库存
//                            String stockKey = Constant.STOCK_DISH + dishId;
//
//                            // 判断 Redis 中是否有该库存 Key
//                            if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
//                                // 增加库存数量
//                                redisTemplate.opsForValue().increment(stockKey, number);
//
//                                // 设置过期时间为 24 小时，防止内存无限增长
//                                redisTemplate.expire(stockKey, 24, TimeUnit.HOURS);
//
//                                log.info("恢复 Redis 库存成功：商品 ID={}", dishId);
//                                successCount++;
//
//                            }
//
//                        }
//
//                    }
//                }
//                failCount++;
//                // 使用mysql的字段方式来确保数据的幂等性
//                //必须设置在最外层 因为他需要是对这个单号进行幂等性的判断
//                userOrderMapper.markStockRestored(Math.toIntExact(order.getId()));
//                log.info("订单 {} 的库存恢复完成", order.getId());
//                log.info("成功恢复 {} 个订单的库存", successCount);
//                log.info("失败恢复 {} 个订单的库存", failCount);
//            }
//        } catch (Exception e) {
//            log.error("库存恢复失败", e);
//            throw new RuntimeException("库存恢复失败");
//        }
//    }

    // 注意事项：
    // 1. 需要添加恢复库存的 SQL 方法
    // 2. 要考虑并发情况（使用乐观锁）
    // 3. 记录详细的恢复日志，方便排查问题

    // TODO: 【订单统计】每小时统计订单数据并缓存
    // ========================================
    // 练习目标：实时统计订单数据，缓存到 Redis 供前端展示
    //
    // 场景问题：
    // 1. 管理端首页需要展示实时订单数据
    // 2. 每次都查数据库性能差
    // 3. 需要统计多个指标（订单数、营业额、平均客单价等）
    //
    // 实现步骤：
    // 1. 查询最近 1 小时的订单数据
    // 2. 统计：订单总数、营业额、有效订单数、平均客单价
    // 3. 使用 Redis Hash 存储统计结果
    // 4. 设置过期时间 24 小时
    //
    // 代码框架：
     @Scheduled(cron = "* 2 * * * ?") // 两小时执行一次
    public void aggregateHourlyOrderStats() {
        //开始计算
        log.info("开始统计订单数据:{}", LocalDateTime.now());
        try{
            //先获取统计时间，然后开始计算值
            LocalDateTime beginTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();
            //获取对应时间段的数据
            String key= Constant.STATUS_TOTAL_CONSTANTS+endTime.format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
            //调用方法
            Map<String, Object> hourlyStatus = userOrderMapper.getHourlyStatus(beginTime, endTime);
            //如果存在数据
            if(hourlyStatus!=null&&!hourlyStatus.isEmpty())
            {
                //返回对应的数据统计
                log.info("查询到统计数据：订单数={}, 营业额={}, 有效订单={}, 平均客单价={}",
                hourlyStatus.get("orderCount"),hourlyStatus.get("turnover"),
                hourlyStatus.get("validOrderCount"),hourlyStatus.get("avgPrice"));
                //将得到的数据存放至Map中
                redisTemplate.opsForHash().putAll(key,hourlyStatus);
                //设置过期时间
                redisTemplate.expire(key, 24, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.error("订单数据统计失败", e);
        }
    }

    // 前端使用：
    // 管理端首页直接读取 Redis 缓存，无需查询数据库

    // TODO: 【数据同步】定时同步 Redis 和数据库，保证数据一致性
    // ========================================
    // 练习目标：定期将 Redis 中的购物车、库存等数据同步到数据库
    //
    // 场景问题：
    // 1. 购物车数据存在 Redis 中，用户退出后数据可能丢失
    // 2. Redis 库存和数据库库存可能出现不一致
    // 3. 需要定期同步保证数据可靠性
    //
    // 实现步骤：
    // 1. 查询 Redis 中的所有购物车数据
    // 2. 批量写入数据库
    // 3. 处理同步异常（记录日志、重试机制）
    // 4. 使用 Pipeline 提升批量操作性能
    //
    // 代码框架：
    // @Scheduled(cron = "0 */30 * * * ?") // 每 30 分钟执行一次
    // public void syncCacheWithDatabase() {
    //     log.info("开始同步 Redis 缓存到数据库...{}", LocalDateTime.now());
    //     
    //     try {
    //         // 1. 同步购物车数据
    //         syncShoppingCart();
    //         
    //         // 2. 同步库存数据
    //         syncStockData();
    //         
    //         log.info("缓存同步完成");
    //     } catch (Exception e) {
    //         log.error("缓存同步失败", e);
    //     }
    // }
    //
    // // 同步购物车数据
    // private void syncShoppingCart() {
    //     log.info("开始同步购物车数据...");
    //     
    //     // 获取所有用户的购物车 key
    //     // Set<String> cartKeys = redisTemplate.keys("cart:*");
    //     //
    //     // for (String cartKey : cartKeys) {
    //     //     // 提取用户 ID
    //     //     Long userId = Long.parseLong(cartKey.split(":")[1]);
    //     //     
    //     //     // 获取购物车数据
    //     //     Map<Object, Object> cartItems = redisTemplate.opsForHash().entries(cartKey);
    //     //     
    //     //     // 批量写入数据库（使用 Pipeline）
    //     //     redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    //     //         for (Map.Entry<Object, Object> entry : cartItems.entrySet()) {
    //     //             ShoppingCart cart = JSON.parseObject(
    //     //                 entry.getValue().toString(), 
    //     //                 ShoppingCart.class
    //     //             );
    //     //             shoppingCartMapper.insert(cart);
    //     //         }
    //     //         return null;
    //     //     });
    //     // }
    // }
    //
    // // 同步库存数据
    // private void syncStockData() {
    //     log.info("开始同步库存数据...");
    //     
    //     // 获取所有库存 key
    //     // Set<String> stockKeys = redisTemplate.keys("stock:dish:*");
    //     //
    //     // for (String stockKey : stockKeys) {
    //     //     Long dishId = Long.parseLong(stockKey.split(":")[2]);
    //     //     Integer redisStock = Integer.parseInt(
    //     //         redisTemplate.opsForValue().get(stockKey).toString()
    //     //     );
    //     //     
    //     //     // 更新数据库库存
    //     //     // dishMapper.updateStock(dishId, redisStock);
    //     // }
    // }
    //
    // 注意事项：
    // 1. 使用 Pipeline 批量操作，性能提升 10 倍以上
    // 2. 记录同步日志，方便排查问题
    // 3. 考虑使用乐观锁防止并发冲突

}
