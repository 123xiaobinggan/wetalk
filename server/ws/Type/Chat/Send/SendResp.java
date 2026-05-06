package com.wetalk.ws.Type.Chat.Send;

import com.wetalk.ws.Type.Chat.Content.*;

public class SendResp {
    private Long msgId;

    public SendResp() {
    }

    public SendResp(Long msgId) {
        this.msgId = msgId;
    }

    public Long getMsgId() {
        return msgId;
    }

    public void getMsgId(Long msgId) {
        this.msgId = msgId;
    }

}
