package com.wetalk.VO.FileController;

import lombok.Data;

@Data
public class ImagesUploadVO {
    private String[] urls;

    public ImagesUploadVO(String[] urls) {
        this.urls = urls;
    }
}
