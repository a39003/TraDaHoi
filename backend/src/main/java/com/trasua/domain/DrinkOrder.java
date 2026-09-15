package com.trasua.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drink_orders")
public class DrinkOrder extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_member_id", nullable = false)
    private Member payer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_member_id", nullable = false)
    private Member createdBy;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "split_mode", nullable = false, length = 20)
    private String splitMode = "ITEMIZED";

    @Column(length = 500)
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> items = new ArrayList<>();

    public DrinkOrder() {
    }

    public Long getId() { return id; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public Member getPayer() { return payer; }
    public void setPayer(Member payer) { this.payer = payer; }
    public Member getCreatedBy() { return createdBy; }
    public void setCreatedBy(Member createdBy) { this.createdBy = createdBy; }
    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
    public String getSplitMode() { return splitMode; }
    public void setSplitMode(String splitMode) { this.splitMode = splitMode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<OrderItem> getItems() { return items; }

    public void replaceItems(List<OrderItem> replacements) {
        items.clear();
        for (OrderItem item : replacements) {
            item.setOrder(this);
            items.add(item);
        }
    }
}
