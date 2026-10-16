package com.pacgram.messaging.controller;

import com.pacgram.common.api.ApiResponse;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.messaging.dto.ConversationDto;
import com.pacgram.messaging.dto.MessageDto;
import com.pacgram.messaging.dto.SendMessageRequest;
import com.pacgram.messaging.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Messages")
@SecurityRequirement(name = "bearerAuth")
public class MessagingController {
    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping
    @Operation(summary = "List conversations")
    public ApiResponse<Page<ConversationDto>> conversations(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(messagingService.listConversations(principal, pageable));
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "List messages in a conversation")
    public ApiResponse<Page<MessageDto>> messages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(messagingService.listMessages(conversationId, principal, pageable));
    }

    @PostMapping("/start/{userId}")
    @Operation(summary = "Start or reopen a direct conversation")
    public ApiResponse<ConversationDto> start(
            @PathVariable Long userId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.success(messagingService.startConversation(userId, principal));
    }

    @PostMapping("/{conversationId}")
    @Operation(summary = "Send a direct message")
    public ApiResponse<MessageDto> send(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(messagingService.sendMessage(
                conversationId, principal, request.text()));
    }

    @PostMapping("/{conversationId}/read")
    @Operation(summary = "Mark incoming messages as read")
    public ApiResponse<String> markRead(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        messagingService.markRead(conversationId, principal);
        return ApiResponse.success("Conversation marked as read.");
    }
}
