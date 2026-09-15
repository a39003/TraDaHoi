package com.trasua.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record MemberRequest(
        @NotBlank(message = "Tên thành viên không được để trống") @Size(max = 100, message = "Tên thành viên tối đa 100 ký tự") String displayName,
        @Email(message = "Email không đúng định dạng") @Size(max = 150, message = "Email tối đa 150 ký tự") String email,
        @Size(max = 100) String bankName,
        @Size(max = 20) String bankBin,
        @Size(max = 50) String accountNumber,
        @Size(max = 150) String accountName,
        @Size(max = 500) String qrCodeUrl,
        @Size(max = 500) String avatarUrl,
        Boolean active,
        @Pattern(regexp = "ADMIN|MEMBER", message = "Vai trò phải là ADMIN hoặc MEMBER") String role
) {
}
