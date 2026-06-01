package com.wetalk.mq.constant;


public class MqTag {
    // CHAT Topic
    public static final String SEND = "send";
    public static final String RECALL = "recall";

    // CALL Topic
    public static final String AUDIO_CALL = "audio_call";
    public static final String AUDIO_END = "audio_end";

    public static final String VEDIO_CALL = "vedio_call";
    public static final String VEDIO_END= "vedio_end";

    // GROUP Topic
    public static final String CREATE = "create";
    public static final String JOIN = "join";
    public static final String QUIT = "quit";
    public static final String DISBAND = "disband";
    public static final String MEMBERS_UPDATE = "members_update";
    public static final String GROUP_UPDATE = "group_update";
    public static final String SET_ROLE = "set_role";


    // Friend Topic
    public static final String FRIEND_REQUEST = "friend_request";
    public static final String FRIEND_ACCEPT = "friend_accept";
    public static final String FRIEND_UPDATE = "friend_update";

    //System Topic
    public static final String SYSTEM_NOTICE = "system_notice";

    private MqTag(){}
}
    
