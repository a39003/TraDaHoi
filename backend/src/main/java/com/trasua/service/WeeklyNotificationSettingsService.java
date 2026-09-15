package com.trasua.service;

import com.trasua.api.dto.WeeklyNotificationSettingsRequest;
import com.trasua.api.dto.WeeklyNotificationSettingsResponse;
import com.trasua.domain.WeeklyNotificationSettings;
import com.trasua.repository.WeeklyNotificationSettingsRepository;
import com.trasua.support.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyNotificationSettingsService {
    private static final long SETTINGS_ID = 1L;
    private final WeeklyNotificationSettingsRepository settings;

    public WeeklyNotificationSettingsService(WeeklyNotificationSettingsRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public WeeklyNotificationSettings getEntity() {
        return settings.findById(SETTINGS_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình thông báo tuần"));
    }

    @Transactional(readOnly = true)
    public WeeklyNotificationSettingsResponse get() {
        return response(getEntity());
    }

    @Transactional
    public WeeklyNotificationSettingsResponse update(WeeklyNotificationSettingsRequest request) {
        WeeklyNotificationSettings entity = getEntity();
        entity.setSendDay(request.sendDay());
        entity.setSendTime(request.sendTime());
        entity.setDebtorTitle(request.debtorTitle().trim());
        entity.setDebtorBody(request.debtorBody().trim());
        entity.setCreditorTitle(request.creditorTitle().trim());
        entity.setCreditorBody(request.creditorBody().trim());
        settings.saveAndFlush(entity);
        return response(entity);
    }

    public String render(String template, long amount, String creditor, String weekStart) {
        return template
                .replace("{amount}", String.format("%,d d", amount).replace(',', '.'))
                .replace("{creditor}", creditor == null ? "" : creditor)
                .replace("{weekStart}", weekStart);
    }

    private WeeklyNotificationSettingsResponse response(WeeklyNotificationSettings entity) {
        return new WeeklyNotificationSettingsResponse(entity.getSendDay(), entity.getSendTime(),
                entity.getDebtorTitle(), entity.getDebtorBody(), entity.getCreditorTitle(),
                entity.getCreditorBody(), entity.getUpdatedAt());
    }
}
