package com.wetalk.business.conversation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.security.core.Authentication;

import java.util.*;

import com.wetalk.model.*;
import com.wetalk.business.conversation.service.ConversationService;
import com.wetalk.business.conversation.dto.*;
import com.wetalk.business.conversation.vo.*;
import com.wetalk.vo.HttpResult;

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
