package com.prandiumnutri.controller;

import com.prandiumnutri.dto.ChatMessageDto;
import com.prandiumnutri.dto.ChatMetaDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CurrentUser currentUser;

    @GetMapping("/{linkId}")
    public List<ChatMessageDto> list(@PathVariable Long linkId) {
        return chatService.list(currentUser.get(), linkId);
    }

    @GetMapping("/{linkId}/meta")
    public ChatMetaDto meta(@PathVariable Long linkId) {
        return chatService.meta(currentUser.get(), linkId);
    }

    @PostMapping("/{linkId}")
    public ChatMessageDto send(@PathVariable Long linkId, @RequestBody Map<String, String> body) {
        return chatService.send(currentUser.get(), linkId, body.get("content"));
    }

    @PostMapping("/{linkId}/read")
    public Map<String, Integer> markRead(@PathVariable Long linkId) {
        return Map.of("updated", chatService.markRead(currentUser.get(), linkId));
    }

    @GetMapping("/{linkId}/unread")
    public Map<String, Long> unread(@PathVariable Long linkId) {
        return Map.of("count", chatService.unreadCount(currentUser.get(), linkId));
    }

    @GetMapping("/unread")
    public Map<String, Long> unreadTotal() {
        return Map.of("count", chatService.unreadTotal(currentUser.id()));
    }

    @GetMapping(value = "/{linkId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long linkId) {
        return chatService.subscribe(currentUser.get(), linkId);
    }
}
