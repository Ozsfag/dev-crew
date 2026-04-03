package org.blacksoil.devcrew.bootstrap;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Конфигурация асинхронного выполнения агентов. Использует виртуальные треды Java 21 — дёшево, не
 * блокируют carrier thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "agentExecutor")
  public Executor agentExecutor() {
    // SimpleAsyncTaskExecutor с virtualThreads=true создаёт новый VirtualThread на каждый вызов
    var executor = new SimpleAsyncTaskExecutor("agent-");
    executor.setVirtualThreads(true);
    return executor;
  }
}
