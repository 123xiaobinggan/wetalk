package com.wetalk.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String accountId;
    private String username;
    private String password;
    private String avatar;
    private Integer sex;
    private String personalSignature;
    private String areaName;
    private String areaCode;
    private LocalDateTime createdTime;

    public User() {
    }

    public User(String accountId, String username,
            String password, String avatar, Integer sex,
            String areaName, String areaCode) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.avatar = avatar;
        this.sex = sex;
        this.areaName = areaName;
        this.areaCode = areaCode;
        this.createdTime = LocalDateTime.now();
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPersonalSignature() {
        return this.personalSignature;
    }

    public void setPersonalSignature(String personalSignature) {
        this.personalSignature = personalSignature;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getAreaName() {
        return this.areaName;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaCode() {
        return this.areaCode;
    }

    public Integer getSex() {
        return this.sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public LocalDateTime getCreatedTime() {
        return this.createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
