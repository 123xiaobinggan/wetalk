package com.wetalk.netty.handler;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.wetalk.netty.constant.ChannelAttrKey;
import com.wetalk.netty.session.SessionManager;
import com.wetalk.utils.JwtUtil;
import com.wetalk.utils.Cache;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
@ChannelHandler.Sharable
public class AuthHandler extends ChannelInboundHandlerAdapter {

    private final JwtUtil jwtUtil;
    private final Cache cache;
    private final SessionManager sessionManager;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }

        FullHttpRequest request = (FullHttpRequest) msg;
        String path = new URI(request.uri()).getPath();

        // 只拦截 /ws 握手请求，其他 HTTP 请求直接放行
        if (!"/ws".equals(path)) {
            ctx.fireChannelRead(msg);
            return;
        }

        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            sendError(ctx, HttpResponseStatus.UNAUTHORIZED, "token missing");
            return;
        }

        System.out.println("AuthHandler 收到token " + token);

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            sendError(ctx, HttpResponseStatus.UNAUTHORIZED, "invalid token");
            return;
        }

        Object redisToken = cache.getToken(userId);
        if (redisToken == null) {
            sendError(ctx, HttpResponseStatus.UNAUTHORIZED, "login expired");
            return;
        }

        if (!token.equals(redisToken.toString())) {
            sendError(ctx, HttpResponseStatus.UNAUTHORIZED, "token mismatch");
            return;
        }

        ctx.channel().attr(ChannelAttrKey.USER_ID).set(userId);
        sessionManager.bind(userId, ctx.channel());

        System.out.println("WebSocket鉴权成功, userId=" + userId + "，准备继续握手");

        ctx.pipeline().remove(this);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Long userId = ctx.channel().attr(ChannelAttrKey.USER_ID).get();
        if (userId != null) {
            sessionManager.unbind(userId, ctx.channel());
            System.out.println("AuthHandler: 连接断开, userId=" + userId);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private String extractToken(FullHttpRequest request) {
        String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (authHeader != null && !authHeader.isBlank()) {
            String prefix = "Bearer ";
            if (authHeader.startsWith(prefix)) {
                String token = authHeader.substring(prefix.length()).trim();
                if (!token.isBlank()) {
                    return token;
                }
            }
        }

        return null;
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String msg) {
        byte[] bytes = msg.getBytes(CharsetUtil.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        response.content().writeBytes(bytes);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
