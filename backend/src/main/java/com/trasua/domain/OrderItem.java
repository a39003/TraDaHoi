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
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private DrinkOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumer_member_id", nullable = false)
    private Member consumer;

    @Column(name = "drink_name", nullable = false, length = 150)
    private String drinkName;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    public OrderItem() {
    }

    public Long getId() { return id; }
    public DrinkOrder getOrder() { return order; }
    public void setOrder(DrinkOrder order) { this.order = order; }
    public Member getConsumer() { return consumer; }
    public void setConsumer(Member consumer) { this.consumer = consumer; }
    public String getDrinkName() { return drinkName; }
    public void setDrinkName(String drinkName) { this.drinkName = drinkName; }
    public long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(long unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public long getLineTotal() { return lineTotal; }
    public void setLineTotal(long lineTotal) { this.lineTotal = lineTotal; }
}
