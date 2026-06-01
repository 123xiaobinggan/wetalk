package com.wetalk.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;

import com.wetalk.model.User;
import com.wetalk.utils.JwtUtil;
import com.wetalk.utils.Cache;
import com.wetalk.mapper.UserMapper;

import java.io.IOException;
import java.util.ArrayList;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final Cache cache;
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return pathMatcher.match("/api/enter", path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {

                final Long userId = jwtUtil.getUserId(token);

                String redisToken = cache.getToken(userId);

                if (redisToken != null && !redisToken.equals(token)) {
                    System.out.println("token失效 " + "redisToken: " + redisToken);
                    filterChain.doFilter(request, response);
                    return;
                }

                User user = cache.getUserOrLoad(userId, () -> {
                    return userMapper.getUserByUserId(userId);
                });

                if (jwtUtil.shouldRefresh(token)) {
                    System.out.println("token 应该更新");
                    token = jwtUtil.generateToken(userId, user.getUsername());

                    cache.setToken(userId, token);
                }

                request.setAttribute("token", token);

                if (user != null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            new ArrayList<>());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                System.out.println("过期的token");
            }

        }

        filterChain.doFilter(request, response);
    }
}
