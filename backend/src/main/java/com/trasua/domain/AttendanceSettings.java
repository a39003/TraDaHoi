package com.trasua.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "attendance_settings")
public class AttendanceSettings {
    @Id
    private Long id;

    @Column(name = "cutoff_time", nullable = false)
    private String cutoffTime;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCutoffTime() { return cutoffTime; }
    public void setCutoffTime(String cutoffTime) { this.cutoffTime = cutoffTime; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
