package ee.dragon.dragontrainer.services;

import ee.dragon.dragontrainer.gameclient.GameApiClient;
import ee.dragon.dragontrainer.gameclient.dto.BuyItemResponse;
import ee.dragon.dragontrainer.gameclient.dto.ShopItemResponse;
import ee.dragon.dragontrainer.gameclient.dto.ShopItemsResponse;
import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.model.ShopItem;
import ee.dragon.dragontrainer.model.ShopPurchase;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.ShopItemRepository;
import ee.dragon.dragontrainer.repository.ShopPurchaseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShopService {

	private final GameApiClient gameApiClient;
	private final GameSessionRepository gameSessionRepository;
	private final ShopItemRepository shopItemRepository;
	private final ShopPurchaseRepository shopPurchaseRepository;

	public ShopService(
			GameApiClient gameApiClient,
			GameSessionRepository gameSessionRepository,
			ShopItemRepository shopItemRepository,
			ShopPurchaseRepository shopPurchaseRepository) {
		this.gameApiClient = gameApiClient;
		this.gameSessionRepository = gameSessionRepository;
		this.shopItemRepository = shopItemRepository;
		this.shopPurchaseRepository = shopPurchaseRepository;
	}

	@Transactional
	public List<ShopItem> fetchAndStoreShopItems(UUID gameSessionId) {
		GameSession session = loadSession(gameSessionId);
		ShopItemsResponse response = gameApiClient.getShopItems(session.getExternalGameId());
		shopItemRepository.deleteByGameSession_Id(gameSessionId);

		List<ShopItem> items = response.items().stream()
				.map(item -> toShopItem(session, item))
				.toList();
		return shopItemRepository.saveAll(items);
	}

	@Transactional
	public ShopPurchase buyItem(UUID gameSessionId, String externalItemId) {
		GameSession session = loadSession(gameSessionId);
		BuyItemResponse response = gameApiClient.buyItem(session.getExternalGameId(), externalItemId);
		ShopPurchase purchase = new ShopPurchase(
				session,
				externalItemId,
				response.shoppingSuccess(),
				response.gold(),
				response.lives(),
				response.level(),
				response.turn());
		return shopPurchaseRepository.save(purchase);
	}

	private ShopItem toShopItem(GameSession session, ShopItemResponse item) {
		return new ShopItem(session, item.id(), item.name(), item.cost());
	}

	private GameSession loadSession(UUID gameSessionId) {
		return gameSessionRepository
				.findById(gameSessionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));
	}
}
