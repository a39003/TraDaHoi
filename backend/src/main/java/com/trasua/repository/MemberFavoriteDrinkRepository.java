package com.trasua.repository;

import com.trasua.domain.MemberFavoriteDrink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemberFavoriteDrinkRepository extends JpaRepository<MemberFavoriteDrink, MemberFavoriteDrink.Key> {
    List<MemberFavoriteDrink> findByMemberIdOrderByCreatedAtAsc(Long memberId);
    boolean existsByMemberIdAndDrinkId(Long memberId, Long drinkId);
    void deleteByMemberIdAndDrinkId(Long memberId, Long drinkId);
}
