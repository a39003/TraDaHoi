package com.trasua.repository;

import com.trasua.domain.DrinkCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DrinkCatalogRepository extends JpaRepository<DrinkCatalogItem, Long> {
    List<DrinkCatalogItem> findByActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    Optional<DrinkCatalogItem> findByNameIgnoreCase(String name);
}
