package com.trasua.repository;

import com.trasua.domain.WeeklySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklySettlementRepository extends JpaRepository<WeeklySettlement, Long> {
    Optional<WeeklySettlement> findByWeekStart(LocalDate weekStart);
    Optional<WeeklySettlement> findFirstByOrderByWeekStartDesc();
}
