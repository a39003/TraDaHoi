package com.trasua.api;

import com.trasua.api.dto.DrinkCatalogResponse;
import com.trasua.domain.DrinkCatalogItem;
import com.trasua.domain.MemberFavoriteDrink;
import com.trasua.repository.DrinkCatalogRepository;
import com.trasua.repository.MemberFavoriteDrinkRepository;
import com.trasua.service.AuthService;
import com.trasua.support.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members/me/favorite-drinks")
public class FavoriteDrinkController {
    private final MemberFavoriteDrinkRepository favorites;
    private final DrinkCatalogRepository drinks;
    private final AuthService auth;

    public FavoriteDrinkController(MemberFavoriteDrinkRepository favorites, DrinkCatalogRepository drinks, AuthService auth) {
        this.favorites = favorites;
        this.drinks = drinks;
        this.auth = auth;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<DrinkCatalogResponse> list(@RequestHeader("Authorization") String authorization) {
        Long memberId = auth.currentMember(authorization).getId();
        return favorites.findByMemberIdOrderByCreatedAtAsc(memberId).stream().map(item -> response(item.getDrink())).toList();
    }

    @PutMapping("/{drinkId}")
    @Transactional
    public DrinkCatalogResponse add(@RequestHeader("Authorization") String authorization, @PathVariable Long drinkId) {
        var member = auth.currentMember(authorization);
        DrinkCatalogItem drink = drinks.findById(drinkId).filter(DrinkCatalogItem::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đồ uống đang hoạt động"));
        if (!favorites.existsByMemberIdAndDrinkId(member.getId(), drinkId)) {
            MemberFavoriteDrink favorite = new MemberFavoriteDrink();
            favorite.setMember(member); favorite.setDrink(drink); favorites.save(favorite);
        }
        return response(drink);
    }

    @DeleteMapping("/{drinkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remove(@RequestHeader("Authorization") String authorization, @PathVariable Long drinkId) {
        favorites.deleteByMemberIdAndDrinkId(auth.currentMember(authorization).getId(), drinkId);
    }

    private DrinkCatalogResponse response(DrinkCatalogItem drink) {
        return new DrinkCatalogResponse(drink.getId(), drink.getName(), drink.getPrice(), drink.getIcon(), drink.isActive());
    }
}
