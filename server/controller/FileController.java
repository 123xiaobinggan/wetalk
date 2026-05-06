package com.wetalk.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.wetalk.service.FileService;
import com.wetalk.VO.HttpResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/uploadAvatars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HttpResult<Object> uploadAvatars(@RequestParam("avatars") MultipartFile[] avatars,
            @RequestParam("names") String[] names) {
        return fileService.uploadAvatars(avatars, names);
    }

    @PostMapping("/uploadMedia")
    public HttpResult<Object> uploadMedia(@RequestParam("media") MultipartFile media,
            @RequestParam("msgType") Integer msgType,
            @RequestParam("senderId") Long senderId, @RequestParam("convId") Long convId) {
        return fileService.uploadMedia(media, msgType, senderId, convId);
    }
}
