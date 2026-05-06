package com.wetalk.netty.constant;

import io.netty.util.AttributeKey;
import com.wetalk.netty.handler.*;

public class ChannelAttrKey {

    public static final  AttributeKey<HeartbeatHandler.HeartbeatInfo> HEARTBEAT_INFO = 
        AttributeKey.valueOf("heartbeatInfo");

    public static final AttributeKey<String> TOKEN = AttributeKey.valueOf("token");

    public static final AttributeKey<Long> USER_ID = AttributeKey.valueOf("userId");
}