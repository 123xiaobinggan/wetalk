package com.wetalk.VO.ConversationController;

import java.time.LocalDateTime;

public class ConversationClearChatHistoryVO {
    private LocalDateTime lastClearedTime;

    public ConversationClearChatHistoryVO(LocalDateTime lastClearedTime) {
        this.lastClearedTime = lastClearedTime;
    }

    public LocalDateTime getLastClearedTime() {
        return lastClearedTime;
    }
}
