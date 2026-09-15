package com.trasua.service;

import com.trasua.api.dto.DrinkCatalogRequest;
import com.trasua.api.dto.DrinkCatalogResponse;
import com.trasua.domain.DrinkCatalogItem;
import com.trasua.repository.DrinkCatalogRepository;
import com.trasua.support.BusinessRuleException;
import com.trasua.support.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DrinkCatalogService {
    private final DrinkCatalogRepository drinks;
    public DrinkCatalogService(DrinkCatalogRepository drinks) { this.drinks = drinks; }
    @Transactional(readOnly = true) public List<DrinkCatalogResponse> list(boolean includeInactive) {
        return (includeInactive ? drinks.findAll() : drinks.findByActiveTrueOrderByNameAsc()).stream().map(this::response).toList();
    }
    @Transactional public DrinkCatalogResponse create(DrinkCatalogRequest request) {
        if (drinks.existsByNameIgnoreCase(request.name().trim())) throw new BusinessRuleException("Đồ uống này đã tồn tại");
        return response(drinks.save(apply(new DrinkCatalogItem(), request)));
    }
    @Transactional public DrinkCatalogResponse update(Long id, DrinkCatalogRequest request) {
        DrinkCatalogItem item = drinks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đồ uống"));
        if (drinks.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) throw new BusinessRuleException("Đồ uống này đã tồn tại");
        return response(apply(item, request));
    }
    @Transactional public void deletePermanently(Long id) {
        DrinkCatalogItem item = drinks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đồ uống"));
        drinks.delete(item);
        drinks.flush();
    }
    private DrinkCatalogItem apply(DrinkCatalogItem item, DrinkCatalogRequest request) {
        item.setName(request.name().trim()); item.setPrice(request.price()); item.setIcon(request.icon() == null ? null : request.icon().trim());
        if (request.active() != null) item.setActive(request.active()); return item;
    }
    private DrinkCatalogResponse response(DrinkCatalogItem item) { return new DrinkCatalogResponse(item.getId(), item.getName(), item.getPrice(), item.getIcon(), item.isActive()); }
}
