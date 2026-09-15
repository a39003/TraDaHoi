package com.trasua.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "settlement_balances")
public class SettlementBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private WeeklySettlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "consumed_amount", nullable = false)
    private long consumedAmount;

    @Column(name = "paid_amount", nullable = false)
    private long paidAmount;

    @Column(name = "net_amount", nullable = false)
    private long netAmount;

    public SettlementBalance() {
    }

    public Long getId() { return id; }
    public WeeklySettlement getSettlement() { return settlement; }
    public void setSettlement(WeeklySettlement settlement) { this.settlement = settlement; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public long getConsumedAmount() { return consumedAmount; }
    public void setConsumedAmount(long consumedAmount) { this.consumedAmount = consumedAmount; }
    public long getPaidAmount() { return paidAmount; }
    public void setPaidAmount(long paidAmount) { this.paidAmount = paidAmount; }
    public long getNetAmount() { return netAmount; }
    public void setNetAmount(long netAmount) { this.netAmount = netAmount; }
}
