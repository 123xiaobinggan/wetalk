package com.wetalk.DTO.UserController;

import lombok.Data;

@Data
public class EnterDTO {
    private String accountId;
    private String username;
    private String password;
    private Boolean isLogin;
    private Integer sex;
    private String areaName;
    private String areaCode;

    public EnterDTO(){}

    public String getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Boolean getIsLogin() {
        return isLogin;
    }

    public Integer getSex() {
        return sex;
    }

    public String getAreaName() {
        return areaName;
    }

    public String getAreaCode() {
        return areaCode;
    }

}
