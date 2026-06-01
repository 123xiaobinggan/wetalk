package com.wetalk.aop.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BusinessLog {
    /**
     * 业务名称，例如：发送消息、撤回消息、通话结束、普通通知推送
     */
    String value() default "";

    /**
     * 是否打印入参
     */
    boolean logArgs() default true;

    /**
     * 是否打印返回值
     */
    boolean logResult() default false;

    /**
     * 慢方法阈值，单位 ms
     */
    long slowThresholdMs() default 500;
}