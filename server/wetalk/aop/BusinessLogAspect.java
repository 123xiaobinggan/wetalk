package com.wetalk.aop;

import com.alibaba.fastjson2.JSON;
import com.wetalk.aop.annotation.BusinessLog;
import com.wetalk.mq.protocol.WsEvent;
import com.wetalk.mq.protocol.NotifyPush;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class BusinessLogAspect {

    /**
     * 业务 Service 和 Consumer 切点
     */
    @Pointcut(
            "execution(* com.wetalk.business..service..*(..)) " +
            "|| execution(* com.wetalk.business..consumer..*(..)) " +
            "|| @annotation(com.wetalk.aop.annotation.BusinessLog)"
    )
    public void businessPointcut() {
    }

    @Around("businessPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        BusinessLog businessLog = method.getAnnotation(BusinessLog.class);

        String traceId = buildTraceId(joinPoint.getArgs());
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        String bizName = businessLog != null && !businessLog.value().isBlank()
                ? businessLog.value()
                : className + "." + methodName;

        boolean logArgs = businessLog == null || businessLog.logArgs();
        boolean logResult = businessLog != null && businessLog.logResult();
        long slowThresholdMs = businessLog == null ? 500 : businessLog.slowThresholdMs();

        try {
            if (logArgs) {
                log.info("[BUSINESS_START] traceId={}, biz={}, method={}.{}, args={}",
                        traceId,
                        bizName,
                        className,
                        methodName,
                        safeArgs(joinPoint.getArgs()));
            } else {
                log.info("[BUSINESS_START] traceId={}, biz={}, method={}.{}",
                        traceId,
                        bizName,
                        className,
                        methodName);
            }

            Object result = joinPoint.proceed();

            long cost = System.currentTimeMillis() - start;

            if (cost >= slowThresholdMs) {
                log.warn("[BUSINESS_SLOW] traceId={}, biz={}, method={}.{}, cost={}ms, threshold={}ms",
                        traceId,
                        bizName,
                        className,
                        methodName,
                        cost,
                        slowThresholdMs);
            }

            if (logResult) {
                log.info("[BUSINESS_END] traceId={}, biz={}, method={}.{}, cost={}ms, result={}",
                        traceId,
                        bizName,
                        className,
                        methodName,
                        cost,
                        safeJson(result));
            } else {
                log.info("[BUSINESS_END] traceId={}, biz={}, method={}.{}, cost={}ms",
                        traceId,
                        bizName,
                        className,
                        methodName,
                        cost);
            }

            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;

            log.error("[BUSINESS_ERROR] traceId={}, biz={}, method={}.{}, cost={}ms, error={}",
                    traceId,
                    bizName,
                    className,
                    methodName,
                    cost,
                    e.getMessage(),
                    e);

            throw e;
        }
    }

    private String buildTraceId(Object[] args) {
        if (args == null) {
            return UUID.randomUUID().toString();
        }

        for (Object arg : args) {
            if (arg instanceof NotifyPush notifyPush) {
                return "notifyPush-" + System.currentTimeMillis();
            }
        }

        return UUID.randomUUID().toString();
    }

    private String safeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        try {
            String json = JSON.toJSONString(args);

            // 防止日志过长
            if (json.length() > 1000) {
                return json.substring(0, 1000) + "...";
            }

            return json;
        } catch (Exception e) {
            return "[args serialize failed: " + e.getMessage() + "]";
        }
    }

    private String safeJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        try {
            String json = JSON.toJSONString(obj);

            if (json.length() > 1000) {
                return json.substring(0, 1000) + "...";
            }

            return json;
        } catch (Exception e) {
            return "[result serialize failed: " + e.getMessage() + "]";
        }
    }
}