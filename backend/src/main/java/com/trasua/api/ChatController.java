package com.trasua.api;

import com.trasua.api.dto.ChatMessageRequest;
import com.trasua.api.dto.ChatMessageResponse;
import com.trasua.api.dto.ChatUnreadResponse;
import com.trasua.api.dto.ChatReactionRequest;
import com.trasua.service.ChatMediaContent;
import com.trasua.service.ChatService;
import com.trasua.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/chat/messages")
public class ChatController {
    private final ChatService chat;
    private final AuthService auth;

    public ChatController(ChatService chat, AuthService auth) {
        this.chat = chat;
        this.auth = auth;
    }

    @GetMapping
    public List<ChatMessageResponse> list(@RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(defaultValue = "50") int limit) {
        auth.currentMember(authorization);
        return chat.list(before, limit);
    }

    @GetMapping("/search")
    public List<ChatMessageResponse> search(@RequestHeader("Authorization") String authorization,
                                            @RequestParam String query,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
                                            @RequestParam(defaultValue = "50") int limit) {
        auth.currentMember(authorization);
        return chat.search(query, before, limit);
    }

    @GetMapping("/unread-count")
    public ChatUnreadResponse unreadCount(@RequestHeader("Authorization") String authorization) {
        return chat.unreadCount(auth.currentMember(authorization));
    }

    @PatchMapping("/read")
    public ChatUnreadResponse markRead(@RequestHeader("Authorization") String authorization) {
        return chat.markRead(auth.currentMember(authorization));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse send(@RequestHeader("Authorization") String authorization,
                                    @Valid @RequestBody ChatMessageRequest request) {
        return chat.send(request, auth.currentMember(authorization));
    }

    @PostMapping(value = "/with-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendWithMedia(
            @RequestHeader("Authorization") String authorization,
            @RequestParam Long senderMemberId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Long replyToMessageId,
            @RequestParam(name = "images", required = false) List<MultipartFile> images) {
        return chat.sendWithMedia(auth.currentMember(authorization), content, replyToMessageId, images);
    }

    @DeleteMapping("/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authorization,
                       @PathVariable Long messageId) {
        chat.delete(messageId, auth.currentMember(authorization));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearGroupChat(@RequestHeader("Authorization") String authorization) {
        auth.requireAdmin(authorization);
        chat.clearGroupChat();
    }

    @PostMapping("/{messageId}/reactions")
    public ChatMessageResponse toggleReaction(@RequestHeader("Authorization") String authorization,
                                              @PathVariable Long messageId,
                                              @Valid @RequestBody ChatReactionRequest request) {
        return chat.toggleReaction(messageId, request, auth.currentMember(authorization));
    }

    @GetMapping("/attachments/{attachmentId}/content")
    public ResponseEntity<Resource> attachmentContent(@PathVariable Long attachmentId) {
        ChatMediaContent media = chat.attachmentContent(attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .contentLength(media.sizeBytes())
                .header("X-Content-Type-Options", "nosniff")
                .body(media.resource());
    }
}
