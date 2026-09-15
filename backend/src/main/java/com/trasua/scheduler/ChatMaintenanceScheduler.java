package com.trasua.scheduler;

import com.trasua.service.ChatMaintenanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ChatMaintenanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(ChatMaintenanceScheduler.class);
    private final ChatMaintenanceService maintenance;

    public ChatMaintenanceScheduler(ChatMaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    @Scheduled(cron = "${app.chat.cleanup-cron:0 15 3 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void cleanupOldChatData() {
        try {
            maintenance.cleanup();
        } catch (RuntimeException error) {
            log.error("Khong the don du lieu chat cu", error);
        }
    }
}
