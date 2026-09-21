package com.trasua.api;

import com.trasua.api.dto.MemberRequest;
import com.trasua.api.dto.MemberResponse;
import com.trasua.api.dto.AdminCreateMemberRequest;
import com.trasua.service.MemberService;
import com.trasua.service.AuthService;
import com.trasua.service.ChatMediaStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService members;
    private final AuthService auth;
    private final ChatMediaStorageService media;

    public MemberController(MemberService members, AuthService auth, ChatMediaStorageService media) {
        this.members = members;
        this.auth = auth;
        this.media = media;
    }

    @GetMapping
    public List<MemberResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return members.list(includeInactive).stream().map(ApiMapper::member).toList();
    }

    @GetMapping("/{id}")
    public MemberResponse get(@PathVariable Long id) {
        return ApiMapper.member(members.getRequired(id));
    }

    @GetMapping("/me")
    public MemberResponse me(@RequestHeader("Authorization") String authorization) {
        return ApiMapper.member(auth.currentMember(authorization));
    }

    @PutMapping("/me")
    public MemberResponse updateMe(@RequestHeader("Authorization") String authorization, @Valid @RequestBody MemberRequest request) {
        var current = auth.currentMember(authorization);
        MemberRequest safeRequest = new MemberRequest(request.displayName(), current.getEmail(), request.bankName(),
                request.bankBin(), request.accountNumber(), request.accountName(), request.qrCodeUrl(),
                current.getAvatarUrl(), current.isActive(), current.getRole());
        return ApiMapper.member(members.update(current.getId(), safeRequest));
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public MemberResponse uploadAvatar(@RequestHeader("Authorization") String authorization,
                                       @RequestPart("image") MultipartFile image) {
        var current = auth.currentMember(authorization);
        var prepared = media.prepare(image);
        return ApiMapper.member(members.updateAvatar(current.getId(), prepared.bytes(), prepared.contentType()));
    }

    @GetMapping("/avatar/member/{id}")
    public ResponseEntity<byte[]> memberAvatar(@PathVariable Long id) {
        var avatar = members.avatarContent(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(avatar.bytes());
    }

    @GetMapping("/avatar/{storageKey:.+}")
    public ResponseEntity<Resource> avatar(@PathVariable String storageKey) {
        Resource resource = media.load(storageKey);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType(storageKey)))
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@RequestHeader("Authorization") String authorization, @Valid @RequestBody AdminCreateMemberRequest request) {
        auth.requireAdmin(authorization);
        return ApiMapper.member(members.createAccount(request));
    }

    @PutMapping("/{id}")
    public MemberResponse update(@RequestHeader("Authorization") String authorization, @PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        auth.requireAdmin(authorization);
        return ApiMapper.member(members.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        var actor = auth.currentMember(authorization);
        if (!"ADMIN".equals(actor.getRole())) throw new com.trasua.support.BusinessRuleException("Bạn không có quyền quản trị");
        members.deletePermanently(id, actor.getId());
    }
}
