package com.wetalk.ws.Type.Friend.RequestSend;

public class RequestSendResp {
    private Long friendRequestId;
    private Long requesterUserId;
    private Long userId;
    private String accountId;
    private String username;
    private String avatar;
    private String requestMsg;
    private int status;

    public RequestSendResp() {
    }

    public RequestSendResp(Long friendRequestId, Long requesterUserId, Long userId, String accountId, String username,
            String avatar,
            String requestMsg, int status) {
        this.friendRequestId = friendRequestId;
        this.requesterUserId = requesterUserId;
        this.userId = userId;
        this.accountId = accountId;
        this.username = username;
        this.avatar = avatar;
        this.requestMsg = requestMsg;
        this.status = status;
    }

    public Long getFriendRequestId() {
        return friendRequestId;
    }

    public Long getRequesterUserId() {
        return requesterUserId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getRequestMsg() {
        return requestMsg;
    }

    public int getStatus() {
        return status;
    }

}
