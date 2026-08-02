package com.sky.Task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 自定义定时任务
 */
@Component
@Slf4j
public class MyTask {
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Scheduled(cron = "0 0 1 * * ?")//指定任务的执行时间
    public void executeTask() {
        log.info("开始执行定时任务:{}",new Date());
    }
    
    // TODO: 【缓存预热】每天凌晨 3 点执行
    // 练习目标：系统启动时预加载热点数据到 Redis
    // 步骤：
    // 1. 查询所有启用的分类
    // 2. 查询每个分类下的热门菜品（前 10 个）
    // 3. 写入 Redis 缓存，设置 2 小时过期
    // 提示：使用 redisTemplate.opsForValue().set(key, value, timeout, TimeUnit)
    // @Scheduled(cron = "0 0 3 * * ?")
    // public void cachePreheat() {
    //     log.info("开始缓存预热...");
    //     // TODO: 实现缓存预热逻辑
    //     // List<Category> categories = categoryMapper.list();
    //     // for (Category category : categories) {
    //     //     if (category.getStatus() == 1) {
    //     //         List<DishVO> dishes = dishMapper.listByCategory(category.getId());
    //     //         String key = "dish:" + category.getId();
    //     //         redisTemplate.opsForValue().set(key, dishes, 2, TimeUnit.HOURS);
    //     //     }
    //     // }
    // }
    
    // TODO: 【销量排行榜】每小时执行一次
    // 练习目标：统计菜品销量排行榜，使用 Redis ZSET
    // 步骤：
    // 1. 查询今日各菜品销量
    // 2. 使用 ZSET 存储排行榜
    // 3. key: ranking:dish_sales:{date}
    // 4. score: 销量，value: 菜品ID
    // 5. 设置过期时间 7 天
    // 提示：使用 redisTemplate.opsForZSet().add() 和 incrementScore()
    // @Scheduled(cron = "0 0 * * * ?")
    // public void updateSalesRanking() {
    //     log.info("开始更新销量排行榜...");
    //     // TODO: 实现排行榜统计
    //     // LocalDate today = LocalDate.now();
    //     // String key = "ranking:dish_sales:" + today;
    //     // List<GoodsSalesDTO> salesList = orderMapper.getSalesRanking(today);
    //     // for (GoodsSalesDTO dto : salesList) {
    //     //     redisTemplate.opsForZSet().add(key, dto.getDishId().toString(), dto.getNumber());
    //     // }
    //     // redisTemplate.expire(key, 7, TimeUnit.DAYS);
    // }
    
    // TODO: 【数据清理】每天凌晨 2 点执行
    // 练习目标：清理过期的 Redis 数据，释放内存
    // 步骤：
    // 1. 清理过期的验证码
    // 2. 清理过期的限流数据
    // 3. 清理过期的临时订单
    // 提示：设置合理的 key 过期时间，或使用 DEL 命令手动清理
    // @Scheduled(cron = "0 0 2 * * ?")
    // public void cleanExpiredData() {
    //     log.info("开始清理过期数据...");
    //     // TODO: 实现数据清理逻辑
    //     // Set<String> keys = redisTemplate.keys("verify_code:*");
    //     // redisTemplate.delete(keys);
    // }
    
    // TODO: 【数据统计】每天凌晨 4 点执行
    // 练习目标：汇总昨日营业数据并缓存
    // 步骤：
    // 1. 查询昨日营业额、订单数、用户数
    // 2. 缓存到 Redis
    // 3. key: stats:daily:{date}
    // 提示：使用 Hash 结构存储多个统计指标
    // @Scheduled(cron = "0 0 4 * * ?")
    // public void aggregateDailyStats() {
    //     log.info("开始汇总昨日营业数据...");
    //     // TODO: 实现数据统计逻辑
    //     // LocalDate yesterday = LocalDate.now().minusDays(1);
    //     // String key = "stats:daily:" + yesterday;
    //     // redisTemplate.opsForHash().put(key, "turnover", turnover);
    //     // redisTemplate.opsForHash().put(key, "orderCount", orderCount);
    //     // redisTemplate.expire(key, 30, TimeUnit.DAYS);
    // }
    
    // TODO: 【在线用户统计】每 5 分钟执行一次
    // 练习目标：统计当前在线用户数
    // 步骤：
    // 1. 使用 ZSET 存储用户最后活跃时间
    // 2. key: online:users
    // 3. score: 最后活跃时间戳
    // 4. value: 用户ID
    // 5. 移除超过 30 分钟未活跃的用户
    // 提示：使用 removeRangeByScore 清理过期用户
    // @Scheduled(cron = "0 */5 * * * ?")
    // public void countOnlineUsers() {
    //     log.info("统计在线用户...");
    //     // TODO: 实现在线用户统计
    //     // long expireTime = System.currentTimeMillis() - 30 * 60 * 1000;
    //     // redisTemplate.opsForZSet().removeRangeByScore("online:users", 0, expireTime);
    //     // Long count = redisTemplate.opsForZSet().zCard("online:users");
    //     // log.info("当前在线用户数：{}", count);
    // }
}
