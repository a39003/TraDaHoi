package com.trasua.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WeeklyNotificationSettingsRequest(
        @Min(value = 1, message = "Ngày gửi phải từ Thứ Hai đến Chủ Nhật") @Max(value = 7, message = "Ngày gửi phải từ Thứ Hai đến Chủ Nhật") int sendDay,
        @NotBlank(message = "Giờ gửi không được để trống") @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Giờ gửi phải có dạng HH:mm") String sendTime,
        @NotBlank(message = "Tiêu đề người trả không được để trống") @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String debtorTitle,
        @NotBlank(message = "Nội dung người trả không được để trống") @Size(max = 1000, message = "Nội dung tối đa 1.000 ký tự") String debtorBody,
        @NotBlank(message = "Tiêu đề người nhận không được để trống") @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String creditorTitle,
        @NotBlank(message = "Nội dung người nhận không được để trống") @Size(max = 1000, message = "Nội dung tối đa 1.000 ký tự") String creditorBody
) {
}
