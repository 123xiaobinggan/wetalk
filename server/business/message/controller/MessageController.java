package com.wetalk.business.message.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.security.core.Authentication;

import java.util.*;

import com.wetalk.model.*;
import com.wetalk.business.message.service.MessageService;
import com.wetalk.business.message.dto.*;
import com.wetalk.business.message.vo.*;
import com.wetalk.vo.HttpResult;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class MessageController {
    private final MessageService messageService;

    @PostMapping("/fetchMessages")
    public HttpResult<Object> fetchMessages(@RequestBody MessagesFetchDTO dto) {
        return messageService.fetchMessages(dto);
    }

    @PostMapping("/fetchMessagesByParams")
    public HttpResult<Object> fetchMessagesByParams(@RequestBody MessagesFetchByParamsDTO dto) {
        return messageService.fetchMessagesByParams(dto);
    }

    @PostMapping("/deleteMessage")
    public HttpResult<Object> deleteMessage(@RequestBody MessageDeleteDTO dto) {
        return messageService.handleDeleteMessage(dto);
    }
}
