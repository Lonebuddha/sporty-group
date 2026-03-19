package com.sportygroup.assignment.eventsinput.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

@Configuration
public class VirtualThreadConfig {

    @Bean(destroyMethod = "shutdown")
    ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    TomcatProtocolHandlerCustomizer<ProtocolHandler> protocolHandlerVirtualThreadExecutorCustomizer(
        ExecutorService virtualThreadExecutor
    ) {
        return protocolHandler -> protocolHandler.setExecutor(virtualThreadExecutor);
    }

    @Bean
    AsyncTaskExecutor applicationTaskExecutor(ExecutorService virtualThreadExecutor) {
        return new TaskExecutorAdapter(virtualThreadExecutor);
    }
}

