package com.bgremoval.CodeCafe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures an async thread pool for background-removal processing tasks.
 *
 * <p>{@code @EnableAsync} activates Spring's annotation-driven async execution,
 * allowing {@code @Async("bgRemovalTaskExecutor")} on
 * {@code BackgroundRemovalServiceImpl.process()}.
 *
 * <p>Requirements: 2.3 (concurrent processing)
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * Thread pool used by {@code BackgroundRemovalServiceImpl}.
     *
     * <ul>
     *   <li>Core pool: 2 threads (always alive)</li>
     *   <li>Max pool: number of available processors (scales with the machine)</li>
     *   <li>Queue capacity: 100 (buffers uploads while pool is saturated)</li>
     *   <li>Await termination: 60 s on shutdown</li>
     * </ul>
     */
    @Bean(name = "bgRemovalTaskExecutor")
    public Executor bgRemovalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("bg-removal-");
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
