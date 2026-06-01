package com.wetalk.business.file.vo;

import lombok.Data;

@Data
public class ImagesUploadVO {
    private String[] urls;

    public ImagesUploadVO(String[] urls) {
        this.urls = urls;
    }
}
