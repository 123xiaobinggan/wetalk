package com.wetalk.ws.Type.Call;

import lombok.Data;
import java.util.Map;

@Data
public class CallReq {
    private String callId;
    private String callType;
    private String clientMsgId;

    private Long fromUserId;
    private Long toUserId;

    private Boolean convType;
    private Long sessionId;

    private String remark;
    private String username;
    private String avatar;

    /**
     * WebRTC 数据：
     * audio_call -> { offer: ... }
     * audio_accept -> { answer: ... }
     * audio_candidate -> { candidate: ... }
     * audio_reject -> { reason: ... }
     * audio_end -> { reason: ... }
     */
    private Map<String, Object> p2pData;

    public CallReq() {
    }

    public CallReq(
            String callId,
            String callType,
            String clientMsgId,
            Long fromUserId,
            Long toUserId,
            Boolean convType,
            Long sessionId,
            Map<String, Object> p2pData) {
        this.callId = callId;
        this.callType = callType;
        this.clientMsgId = clientMsgId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.convType = convType;
        this.sessionId = sessionId;
        this.p2pData = p2pData;
    }
}