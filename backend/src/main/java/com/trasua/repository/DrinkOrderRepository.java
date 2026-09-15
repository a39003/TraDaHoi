package com.trasua.repository;

import com.trasua.domain.DrinkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DrinkOrderRepository extends JpaRepository<DrinkOrder, Long> {
    @Query("""
            select distinct o from DrinkOrder o
            join fetch o.payer
            join fetch o.createdBy
            left join fetch o.items i
            left join fetch i.consumer
            where o.orderDate = :date
            order by o.id desc
            """)
    List<DrinkOrder> findDetailedByOrderDate(@Param("date") LocalDate date);

    @Query("""
            select distinct o from DrinkOrder o
            join fetch o.payer
            join fetch o.createdBy
            left join fetch o.items i
            left join fetch i.consumer
            where o.orderDate between :start and :end
            order by o.orderDate asc, o.id asc
            """)
    List<DrinkOrder> findDetailedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select distinct o from DrinkOrder o
            join fetch o.payer
            join fetch o.createdBy
            left join fetch o.items i
            left join fetch i.consumer
            where o.id = :id
            """)
    java.util.Optional<DrinkOrder> findDetailedById(@Param("id") Long id);
}
