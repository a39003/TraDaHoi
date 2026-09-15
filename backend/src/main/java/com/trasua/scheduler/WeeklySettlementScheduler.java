package com.trasua.scheduler;

import com.trasua.service.WeeklyNotificationSettingsService;
import com.trasua.service.WeeklySettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class WeeklySettlementScheduler {
    private static final Logger log = LoggerFactory.getLogger(WeeklySettlementScheduler.class);
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final WeeklySettlementService settlements;
    private final WeeklyNotificationSettingsService settings;

    public WeeklySettlementScheduler(WeeklySettlementService settlements,
                                     WeeklyNotificationSettingsService settings) {
        this.settlements = settlements;
        this.settings = settings;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Ho_Chi_Minh")
    public void closeConfiguredWeek() {
        try {
            LocalDateTime now = LocalDateTime.now(VIETNAM);
            var configuration = settings.get();
            LocalTime configuredTime = LocalTime.parse(configuration.sendTime());
            if (now.getDayOfWeek().getValue() == configuration.sendDay()
                    && now.getHour() == configuredTime.getHour()
                    && now.getMinute() == configuredTime.getMinute()) {
                settlements.finalizeWeek(now.toLocalDate());
            }
        } catch (RuntimeException error) {
            log.error("Khong the chot tuan tra da tu dong", error);
        }
    }
}
