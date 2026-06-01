package com.wetalk.mq.protocol;

import com.wetalk.mq.protocol.NotifyPushUnit;

import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class NotifyPush {
    private List<NotifyPushUnit> notifyPushUnits;

    public NotifyPush(List<NotifyPushUnit> notifyPushUnits){
        this.notifyPushUnits = notifyPushUnits;
    }
}
