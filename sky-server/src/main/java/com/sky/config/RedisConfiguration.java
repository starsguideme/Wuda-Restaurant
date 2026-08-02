package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisConfiguration {

    /** 为 Jackson 创建并配置 ObjectMapper */
    @Bean
    public ObjectMapper jacksonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ParameterNamesModule());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /** RedisTemplate 使用自定义的 ObjectMapper */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper jacksonObjectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(jacksonObjectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
    
    // TODO: 【CacheManager】配置 Spring Cache 统一缓存策略
    // 练习目标：配置 RedisCacheManager，统一设置缓存过期时间和序列化方式
    // 提示：使用 RedisCacheConfiguration.defaultCacheConfig()
    // @Bean
    // public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
    //                                      ObjectMapper jacksonObjectMapper) {
    //     RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
    //             .entryTtl(Duration.ofMinutes(30))
    //             .serializeKeysWith(RedisSerializationContext.SerializationPair
    //                     .fromSerializer(new StringRedisSerializer()))
    //             .serializeValuesWith(RedisSerializationContext.SerializationPair
    //                     .fromSerializer(new GenericJackson2JsonRedisSerializer(jacksonObjectMapper)))
    //             .disableCachingNullValues();
    //     return RedisCacheManager.builder(connectionFactory)
    //             .cacheDefaults(config)
    //             .build();
    // }
    
    // TODO: 【发布订阅】实现 Redis Pub/Sub 消息通知
    // 场景：多实例间消息同步（如订单提醒、库存变更）
    // 练习目标：
    // 1. 配置 RedisMessageListenerContainer
    // 2. 实现 MessageListener 处理消息
    // 3. 使用 redisTemplate.convertAndSend() 发布消息
    // 提示：
    // @Bean
    // public RedisMessageListenerContainer redisMessageListenerContainer(
    //         RedisConnectionFactory connectionFactory,
    //         MessageListenerAdapter listenerAdapter) {
    //     RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    //     container.setConnectionFactory(connectionFactory);
    //     container.addMessageListener(listenerAdapter, new PatternTopic("order:notify"));
    //     return container;
    // }
    
    // TODO: 【延迟队列】实现订单超时自动取消
    // 场景：订单 15 分钟未支付自动取消
    // 练习目标：使用 Redis ZSET 实现延迟队列
    // 提示：
    // - key: delay_queue:order_cancel
    // - score: 订单超时时间戳
    // - value: 订单 ID
    // - 定时任务扫描：redisTemplate.opsForZSet().rangeByScore(key, 0, currentTime)
    // public void addToDelayQueue(Long orderId, Long delaySeconds) {
    //     long score = System.currentTimeMillis() + delaySeconds * 1000;
    //     redisTemplate.opsForZSet().add("delay_queue:order_cancel", orderId.toString(), score);
    // }
    // public void processDelayQueue() {
    //     long now = System.currentTimeMillis();
    //     Set<String> orderIds = redisTemplate.opsForZSet().rangeByScore("delay_queue:order_cancel", 0, now);
    //     // TODO: 处理超时订单并移除
    // }
    
    // TODO: 【布隆过滤器】防止缓存穿透
    // 场景：恶意查询不存在的商品 ID
    // 练习目标：使用 Redisson 实现布隆过滤器
    // 提示：
    // - 引入 Redisson 依赖
    // - RBloomFilter<String> bloomFilter = redisson.getBloomFilter("product:bloom")
    // - bloomFilter.tryInit(1000000, 0.01)
    // - 查询前先判断：bloomFilter.contains(productId)
    // public boolean mightContain(String productId) {
    //     // TODO: 实现布隆过滤器检查
    //     return false;
    // }
    
    // TODO: 【限流器】实现令牌桶限流
    // 场景：API 接口限流，防止恶意请求
    // 练习目标：使用 Redis 实现令牌桶算法
    // 提示：
    // - key: rate_limit:token:{api}
    // - 使用 Lua 脚本保证原子性
    // - 定期检查并补充令牌
    // public boolean tryAcquireToken(String api) {
    //     // TODO: 实现令牌桶限流
    //     return false;
    // }
}
