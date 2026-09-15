package com.trasua.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "weekly_notification_settings")
public class WeeklyNotificationSettings {
    @Id
    private Long id = 1L;

    @Column(name = "send_day", nullable = false)
    private int sendDay;

    @Column(name = "send_time", nullable = false)
    private String sendTime;

    @Column(name = "debtor_title", nullable = false, length = 200)
    private String debtorTitle;

    @Column(name = "debtor_body", nullable = false, length = 1000)
    private String debtorBody;

    @Column(name = "creditor_title", nullable = false, length = 200)
    private String creditorTitle;

    @Column(name = "creditor_body", nullable = false, length = 1000)
    private String creditorBody;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void updated() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public int getSendDay() { return sendDay; }
    public void setSendDay(int sendDay) { this.sendDay = sendDay; }
    public String getSendTime() { return sendTime; }
    public void setSendTime(String sendTime) { this.sendTime = sendTime; }
    public String getDebtorTitle() { return debtorTitle; }
    public void setDebtorTitle(String debtorTitle) { this.debtorTitle = debtorTitle; }
    public String getDebtorBody() { return debtorBody; }
    public void setDebtorBody(String debtorBody) { this.debtorBody = debtorBody; }
    public String getCreditorTitle() { return creditorTitle; }
    public void setCreditorTitle(String creditorTitle) { this.creditorTitle = creditorTitle; }
    public String getCreditorBody() { return creditorBody; }
    public void setCreditorBody(String creditorBody) { this.creditorBody = creditorBody; }
    public Instant getUpdatedAt() { return updatedAt; }
}
