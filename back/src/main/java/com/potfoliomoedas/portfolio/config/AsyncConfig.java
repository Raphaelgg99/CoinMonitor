package com.potfoliomoedas.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Quantas threads ficam sempre ativas (ex: 2 funcionários)
        executor.setCorePoolSize(2);

        // O máximo de threads se a coisa apertar (ex: contrata +2 temporários)
        executor.setMaxPoolSize(5);

        // Quantos e-mails aguentam esperar na fila antes de dar erro
        executor.setQueueCapacity(500);

        executor.setThreadNamePrefix("EmailThread-");
        executor.initialize();
        return executor;
    }
}