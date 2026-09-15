package com.trasua.repository;

import com.trasua.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByActiveTrueOrderByDisplayNameAsc();
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    Optional<Member> findByEmailIgnoreCase(String email);

    @Query("select m.avatarUrl from Member m where m.avatarUrl is not null")
    List<String> findAllAvatarUrls();
}
