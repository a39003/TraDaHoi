package com.trasua.api;

import com.trasua.api.dto.DrinkCatalogRequest;
import com.trasua.api.dto.DrinkCatalogResponse;
import com.trasua.service.DrinkCatalogService;
import com.trasua.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/drinks")
public class DrinkCatalogController {
    private final DrinkCatalogService drinks;
    private final AuthService auth;
    public DrinkCatalogController(DrinkCatalogService drinks, AuthService auth) { this.drinks = drinks; this.auth = auth; }
    @GetMapping public List<DrinkCatalogResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) { return drinks.list(includeInactive); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public DrinkCatalogResponse create(@RequestHeader("Authorization") String authorization, @Valid @RequestBody DrinkCatalogRequest request) { auth.requireAdmin(authorization); return drinks.create(request); }
    @PutMapping("/{id}") public DrinkCatalogResponse update(@RequestHeader("Authorization") String authorization, @PathVariable Long id, @Valid @RequestBody DrinkCatalogRequest request) { auth.requireAdmin(authorization); return drinks.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@RequestHeader("Authorization") String authorization, @PathVariable Long id) { auth.requireAdmin(authorization); drinks.deletePermanently(id); }
}
