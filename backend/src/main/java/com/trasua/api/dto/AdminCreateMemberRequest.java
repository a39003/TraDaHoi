package com.trasua.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateMemberRequest(
        @NotBlank(message = "Tên thành viên không được để trống")
        @Size(max = 100, message = "Tên thành viên tối đa 100 ký tự") String displayName,
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 150, message = "Email tối đa 150 ký tự") String email,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự") String password,
        @Pattern(regexp = "ADMIN|MEMBER", message = "Vai trò phải là ADMIN hoặc MEMBER") String role
) {
}
