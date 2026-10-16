package com.pacgram.messaging.controller;

import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.messaging.dto.TypingRequest;
import com.pacgram.messaging.service.MessagingService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class TypingController {
    private final MessagingService messagingService;

    public TypingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @MessageMapping("/conversations/{conversationId}/typing")
    public void typing(
            @DestinationVariable Long conversationId,
            TypingRequest request,
            Authentication authentication) {
        AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
        messagingService.publishTyping(conversationId, principal, request.typing());
    }
}
