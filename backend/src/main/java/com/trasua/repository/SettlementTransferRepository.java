package com.trasua.repository;

import com.trasua.domain.SettlementTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettlementTransferRepository extends JpaRepository<SettlementTransfer, Long> {
    @Query("""
            select t from SettlementTransfer t
            join fetch t.settlement
            join fetch t.debtor
            join fetch t.creditor
            where t.id = :id
            """)
    java.util.Optional<SettlementTransfer> findDetailedById(@Param("id") Long id);

    @Query("""
            select t from SettlementTransfer t
            join fetch t.debtor
            join fetch t.creditor
            where t.settlement.id = :settlementId
            order by t.id asc
            """)
    List<SettlementTransfer> findDetailedBySettlementId(@Param("settlementId") Long settlementId);

    @Modifying
    @Query("delete from SettlementTransfer t where t.settlement.id = :settlementId")
    void deleteBySettlementId(@Param("settlementId") Long settlementId);
}
