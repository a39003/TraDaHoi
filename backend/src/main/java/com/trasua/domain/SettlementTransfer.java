package com.trasua.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "settlement_transfers")
public class SettlementTransfer extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private WeeklySettlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debtor_member_id", nullable = false)
    private Member debtor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creditor_member_id", nullable = false)
    private Member creditor;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    public SettlementTransfer() {
    }

    public Long getId() { return id; }
    public WeeklySettlement getSettlement() { return settlement; }
    public void setSettlement(WeeklySettlement settlement) { this.settlement = settlement; }
    public Member getDebtor() { return debtor; }
    public void setDebtor(Member debtor) { this.debtor = debtor; }
    public Member getCreditor() { return creditor; }
    public void setCreditor(Member creditor) { this.creditor = creditor; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
