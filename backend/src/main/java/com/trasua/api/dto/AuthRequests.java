package com.trasua.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    private AuthRequests() {}
    public record Register(@NotBlank(message = "Họ tên không được để trống") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String displayName,
                           @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") @Size(max = 150, message = "Email tối đa 150 ký tự") String email,
                           @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự") String password) {}
    public record Login(@NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") @Size(max = 150) String email,
                        @NotBlank(message = "Mật khẩu không được để trống") String password) {}
    public record RequestReset(@NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") @Size(max = 150) String email) {}
    public record ResetPassword(@NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") @Size(max = 150) String email,
                                @NotBlank(message = "Mã đặt lại không được để trống") @Size(min = 6, max = 20, message = "Mã đặt lại phải từ 6 đến 20 ký tự") String code,
                                @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự") String password) {}
}
