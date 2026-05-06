package com.wetalk.netty.initializer;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import com.wetalk.netty.handler.*;
import com.wetalk.utils.JwtUtil;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    
    private final AuthHandler authHandler;
    private final HeartbeatHandler heartbeatHandler;
    private final DispatcherHandler dispatcherHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));

        pipeline.addLast(authHandler);

        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));

        // 心跳处理, 当IdleStateHandler检测空闲时
        pipeline.addLast(new HeartbeatHandler());

        // 分发消息
        pipeline.addLast(dispatcherHandler);
    }
}
