package com.wetalk.business.user.dto;

import lombok.Data;

@Data
public class UpdateUserInfoDTO {
    private Long userId;
    private String oldAccountId;
    private String accountId;
    private String avatar;
    private String password;
    private String newPassword;
    private String personalSignature;
    private String username;
    private Integer sex;
    private String areaName;
    private String areaCode;

    public UpdateUserInfoDTO() {
    }

    public UpdateUserInfoDTO(Long userId, String oldAccountId, String accountId,
            String avatar, String password, String newPassword, String personalSignature,
            String username, Integer sex, String areaName, String areaCode) {
        this.userId = userId;
        this.oldAccountId = oldAccountId;
        this.accountId = accountId;
        this.avatar = avatar;
        this.password = password;
        this.newPassword = newPassword;
        this.personalSignature = personalSignature;
        this.username = username;
        this.sex = sex;
        this.areaName = areaName;
        this.areaCode = areaCode;
    }
}
