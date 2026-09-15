package com.trasua.repository;

import com.trasua.domain.SettlementBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettlementBalanceRepository extends JpaRepository<SettlementBalance, Long> {
    @Query("select b from SettlementBalance b join fetch b.member where b.settlement.id = :settlementId order by b.member.displayName")
    List<SettlementBalance> findDetailedBySettlementId(@Param("settlementId") Long settlementId);

    @Modifying
    @Query("delete from SettlementBalance b where b.settlement.id = :settlementId")
    void deleteBySettlementId(@Param("settlementId") Long settlementId);
}
