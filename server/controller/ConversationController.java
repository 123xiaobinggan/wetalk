package com.wetalk.controller;

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
import com.wetalk.service.ConversationService;

import com.wetalk.DTO.ConversationController.*;
import com.wetalk.VO.ConversationController.*;
import com.wetalk.VO.HttpResult;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ConversationController {
    private final ConversationService conversationService;

    @PostMapping("/fetchConversations")
    public HttpResult<ConversationsFetchVO> fetchConversationsByUserId(@RequestBody ConversationsFetchDTO dto) {
        return conversationService.fetchConversationsByUserId(dto);
    }

    @PostMapping("/getConversation")
    public HttpResult<Object> getCoversation(@RequestBody ConversationGetDTO dto){
        return conversationService.getConversation(dto);
    }

    @PostMapping("/markConversationRead")
    public HttpResult<Object> markConversationRead(@RequestBody ConversationMarkReadDTO dto) {
        return conversationService.markConversationRead(dto);
    }

    @PostMapping("/updateConversation")
    public HttpResult<Object> updateConversation(@RequestBody ConversationUpdateDTO dto) {
        return conversationService.updateConversation(dto);
    }

    @PostMapping("/clearChatHistory")
    public HttpResult<Object> clearChatHistory(@RequestBody ConversationClearChatHistoryDTO dto) {
        return conversationService.clearChatHistory(dto);
    }
}
