package com.wetalk.netty.server;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class NettyRunner implements CommandLineRunner {

    private final NettyServer nettyServer;

    @Override
    public void run(String... args) throws Exception {
        // Spring Boot 启动完成后，这里执行
        nettyServer.start(9090);
    }

    @PreDestroy
    public void destroy() {
        nettyServer.stop();
    }
}