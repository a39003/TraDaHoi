package com.trasua.service;

import com.trasua.api.ApiMapper;
import com.trasua.api.dto.MemberBriefResponse;
import com.trasua.domain.Member;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatTypingService {
    private final ConcurrentHashMap<Long, TypingEntry> typing = new ConcurrentHashMap<>();

    public void update(Member member, boolean active) {
        if (!active) typing.remove(member.getId());
        else typing.put(member.getId(), new TypingEntry(ApiMapper.brief(member), Instant.now().plusSeconds(5)));
    }

    public List<MemberBriefResponse> list(Member actor) {
        Instant now = Instant.now();
        typing.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        return typing.entrySet().stream().filter(entry -> !entry.getKey().equals(actor.getId()))
                .map(entry -> entry.getValue().member()).toList();
    }

    private record TypingEntry(MemberBriefResponse member, Instant expiresAt) {}
}
