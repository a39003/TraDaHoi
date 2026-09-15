package com.trasua.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "member_favorite_drinks")
@IdClass(MemberFavoriteDrink.Key.class)
public class MemberFavoriteDrink {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drink_id")
    private DrinkCatalogItem drink;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void created() { createdAt = Instant.now(); }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public DrinkCatalogItem getDrink() { return drink; }
    public void setDrink(DrinkCatalogItem drink) { this.drink = drink; }

    public static class Key implements Serializable {
        private Long member;
        private Long drink;
        public Key() {}
        @Override public boolean equals(Object value) {
            if (this == value) return true;
            if (!(value instanceof Key other)) return false;
            return java.util.Objects.equals(member, other.member) && java.util.Objects.equals(drink, other.drink);
        }
        @Override public int hashCode() { return java.util.Objects.hash(member, drink); }
    }
}
