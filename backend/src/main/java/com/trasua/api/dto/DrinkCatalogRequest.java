package com.trasua.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DrinkCatalogRequest(@NotBlank(message = "Tên đồ uống không được để trống") @Size(max = 150, message = "Tên đồ uống tối đa 150 ký tự") String name,
                                  @PositiveOrZero(message = "Giá đồ uống không được là số âm") long price,
                                  @Size(max = 20) String icon, Boolean active) {}
