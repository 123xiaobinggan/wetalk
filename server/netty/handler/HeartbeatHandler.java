package com.wetalk.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;

import com.wetalk.netty.constant.ChannelAttrKey;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

@Component
@ChannelHandler.Sharable
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

    // 最大心跳丢失次数（允许连续丢失次数）
    private static final int MAX_LOST_HEARTBEAT_COUNT = 3;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;

            // 获取当前连接的心跳丢失计数
            HeartbeatInfo info = getHeartbeatInfo(ctx);

            System.out.println("HeartbeatHandler: 检测空闲, 心跳处理" + info);

            switch (event.state()) {
                case READER_IDLE:
                    int lostCount = info.incrementLostCount();
                    if (lostCount >= MAX_LOST_HEARTBEAT_COUNT) {
                        ctx.close();
                    }
                    break;
                default:
                    super.userEventTriggered(ctx, evt);
            }
        } else {
            // 其他事件继续传递
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 收到任何消息，说明连接活跃，重置心跳计数
        HeartbeatInfo info = getHeartbeatInfo(ctx);

        // 如果是心跳包，特殊处理
        if (isHeartbeatMessage(msg)) {
            info.resetLostCount();
            sendHeartbeatResponse(ctx); // 响应心跳
            return; // 心跳包不继续传递
        }
        info.resetLostCount();
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 连接建立时，初始化心跳信息
        HeartbeatInfo info = new HeartbeatInfo();
        // 放入心跳信息
        ctx.channel().attr(ChannelAttrKey.HEARTBEAT_INFO).set(info);
        System.out.println("HeartbeatHandler: {} 连接建立" + ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 连接断开时，清理心跳信息
        ctx.channel().attr(ChannelAttrKey.HEARTBEAT_INFO).remove();
        System.out.println("HeartbeatHandler: {} 连接断开" + ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    /**
     * 获取心跳信息
     */
    private HeartbeatInfo getHeartbeatInfo(ChannelHandlerContext ctx) {
        HeartbeatInfo info = ctx.channel().attr(ChannelAttrKey.HEARTBEAT_INFO).get();
        if (info == null) {
            info = new HeartbeatInfo();
            ctx.channel().attr(ChannelAttrKey.HEARTBEAT_INFO).set(info);
        }
        return info;
    }

    /**
     * 判断是否是心跳消息
     */
    private boolean isHeartbeatMessage(Object msg) {
        // 根据你的协议判断，示例：使用 JSON 格式
        if (msg instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) msg).text();
            JSONObject json = JSON.parseObject(text);
            return "ping".equals(json.getString("type"));
        }
        return false;
    }

    private void sendHeartbeatResponse(ChannelHandlerContext ctx) {
        JSONObject pong = new JSONObject();
        pong.put("type", "pong");
        pong.put("timestamp", System.currentTimeMillis());

        ctx.channel().writeAndFlush(new TextWebSocketFrame(pong.toJSONString()));
    }

    /**
     * 心跳信息类: lostCount--失去心跳的次数;
     * lastHeartbearTime-- 最近心跳时间
     */
    public static class HeartbeatInfo {
        private final AtomicInteger lostCount = new AtomicInteger(0);
        private Long lastHeartbeatTime = System.currentTimeMillis();

        public int incrementLostCount() {
            lastHeartbeatTime = System.currentTimeMillis();
            return lostCount.incrementAndGet();
        }

        public void resetLostCount() {
            lostCount.set(0);
            lastHeartbeatTime = System.currentTimeMillis();
        }

        public int getLostCount() {
            return lostCount.get();
        }

        public Long getLastHeartbeatTime() {
            return lastHeartbeatTime;
        }
    }
}