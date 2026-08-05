package com.sky.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {
    @Bean(name="orderTaskExecutor")
    public ThreadPoolTaskExecutor orderTaskExecutor() {
        //设置订单的并发线程池
        ThreadPoolTaskExecutor TaskExecutor = new ThreadPoolTaskExecutor();
        TaskExecutor.setCorePoolSize(3);
        TaskExecutor.setMaxPoolSize(5);
        TaskExecutor.setQueueCapacity(20);
        TaskExecutor.setThreadNamePrefix("order-task-");
        TaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //设置线程池的处于等待时期的活跃时间，超过这个时间，线程会被释放
        TaskExecutor.setAllowCoreThreadTimeOut(true);
        TaskExecutor.setKeepAliveSeconds(60);
        //线程池关闭时会等待线程池中的所有线程都进行关闭后才会关闭
        TaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        TaskExecutor.setAwaitTerminationSeconds(60);

        TaskExecutor.initialize();
        return TaskExecutor;
     }
     @Bean(name="reportTaskExecutor")
     public ThreadPoolTaskExecutor reportTaskExecutor() {
         ThreadPoolTaskExecutor TaskExecutor = new ThreadPoolTaskExecutor();
         TaskExecutor.setCorePoolSize(2);
         TaskExecutor.setMaxPoolSize(3);
         TaskExecutor.setQueueCapacity(10);
         TaskExecutor.setThreadNamePrefix("report-task-");
         TaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
         TaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
         TaskExecutor.setAwaitTerminationSeconds(60);
         //设置线程池的处于等待时期的活跃时间，超过这个时间，线程会被释放
         TaskExecutor.setAllowCoreThreadTimeOut(true);
         TaskExecutor.setKeepAliveSeconds(60);
         //线程池关闭时会等待线程池中的所有线程都进行关闭后才会关闭
         TaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
         TaskExecutor.setAwaitTerminationSeconds(60);
         TaskExecutor.initialize();
         return TaskExecutor;
     }
     @Bean(name="commonTaskExecutor")
     public ThreadPoolTaskExecutor commonTaskExecutor() {
         ThreadPoolTaskExecutor TaskExecutor = new ThreadPoolTaskExecutor();
         TaskExecutor.setCorePoolSize(2);
         TaskExecutor.setMaxPoolSize(4);
         TaskExecutor.setQueueCapacity(8);
         TaskExecutor.setThreadNamePrefix("common-task-");
         TaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
         //设置线程池的处于等待时期的活跃时间，超过这个时间，线程会被释放
         TaskExecutor.setAllowCoreThreadTimeOut(true);
         TaskExecutor.setKeepAliveSeconds(60);
         //线程池关闭时会等待线程池中的所有线程都进行关闭后才会关闭
         TaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
         TaskExecutor.setAwaitTerminationSeconds(60);
         TaskExecutor.initialize();
         return TaskExecutor;
     }
}
