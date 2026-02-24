package com.ITQGroup.doc_stat_history.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@EnableAsync
@EnableScheduling
@Configuration
public class SchedulingConfig {

    // Планировщик с пулом на 2 потока — по одному на каждый воркер.
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // Потоки submitWorker и approveWorker
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("doc-scheduler-");
        // При завершении приложения ждём окончания текущей пачки
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        return scheduler;
    }

    // Отдельный пул для @Async.
    // Используется в воркерах чтобы тело метода не блокировало поток планировщика.
    @Bean(name = "workerExecutor")
    public AsyncTaskExecutor workerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("doc-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}