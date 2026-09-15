package com.trasua.api;

import com.trasua.api.dto.MemberBriefResponse;
import com.trasua.api.dto.TypingRequest;
import com.trasua.service.AuthService;
import com.trasua.service.ChatTypingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat/typing")
public class ChatTypingController {
    private final ChatTypingService typing;
    private final AuthService auth;

    public ChatTypingController(ChatTypingService typing, AuthService auth) {
        this.typing = typing;
        this.auth = auth;
    }

    @GetMapping
    public List<MemberBriefResponse> list(@RequestHeader("Authorization") String authorization) {
        return typing.list(auth.currentMember(authorization));
    }

    @PutMapping
    public void update(@RequestHeader("Authorization") String authorization, @RequestBody TypingRequest request) {
        typing.update(auth.currentMember(authorization), request.typing());
    }
}
