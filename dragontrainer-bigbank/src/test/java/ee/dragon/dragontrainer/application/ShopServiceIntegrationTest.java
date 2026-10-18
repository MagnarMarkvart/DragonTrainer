package ee.dragon.dragontrainer.application;

import static org.assertj.core.api.Assertions.assertThat;

import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.services.ShopService;
import ee.dragon.dragontrainer.model.ShopItem;
import ee.dragon.dragontrainer.model.ShopPurchase;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.ShopItemRepository;
import ee.dragon.dragontrainer.repository.ShopPurchaseRepository;
import java.io.IOException;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShopServiceIntegrationTest {

	private static MockWebServer mockGameApi;

	@Autowired
	private ShopService shopService;

	@Autowired
	private GameSessionRepository gameSessionRepository;

	@Autowired
	private ShopItemRepository shopItemRepository;

	@Autowired
	private ShopPurchaseRepository shopPurchaseRepository;

	@BeforeAll
	static void startMockGameApi() throws IOException {
		mockGameApi = new MockWebServer();
		mockGameApi.start();
	}

	@AfterAll
	static void stopMockGameApi() throws IOException {
		mockGameApi.shutdown();
	}

	@DynamicPropertySource
	static void gameApiProperties(DynamicPropertyRegistry registry) {
		registry.add("game.api.base-url", () -> mockGameApi.url("/").toString().replaceAll("/$", ""));
	}

	@Test
	@Order(1)
	void fetchAndStoreShopItems_callsExternalApiAndPersistsItems() throws InterruptedException {
		GameSession session = gameSessionRepository.save(new GameSession(
				"game-shop-1", 3, 100, 1, 0, 2500, 1));

		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "items": [
						    { "id": "hpot", "name": "Healing potion", "cost": 50 },
						    { "id": "claw", "name": "Claw polish", "cost": 100 }
						  ]
						}
						""")
				.addHeader("Content-Type", "application/json"));

		List<ShopItem> saved = shopService.fetchAndStoreShopItems(session.getId());

		RecordedRequest request = mockGameApi.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/api/v2/game-shop-1/shop");

		assertThat(saved).hasSize(2);
		assertThat(saved).extracting(ShopItem::getExternalItemId).containsExactlyInAnyOrder("hpot", "claw");

		List<ShopItem> fromDb = shopItemRepository.findByGameSession_Id(session.getId());
		assertThat(fromDb).hasSize(2);
	}

	@Test
	@Order(2)
	void buyItem_callsExternalApiAndPersistsPurchase() throws InterruptedException {
		GameSession session = gameSessionRepository.save(new GameSession(
				"game-shop-2", 3, 100, 1, 0, 2500, 1));

		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "shoppingSuccess": "true",
						  "gold": 50,
						  "lives": 4,
						  "level": 2,
						  "turn": 3
						}
						""")
				.addHeader("Content-Type", "application/json"));

		ShopPurchase saved = shopService.buyItem(session.getId(), "hpot");

		RecordedRequest request = mockGameApi.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/api/v2/game-shop-2/shop/buy/hpot");

		assertThat(saved.getShoppingSuccess()).isEqualTo("true");
		assertThat(saved.getGold()).isEqualTo(50);
		assertThat(saved.getLives()).isEqualTo(4);
		assertThat(saved.getLevel()).isEqualTo(2);
		assertThat(saved.getTurn()).isEqualTo(3);

		List<ShopPurchase> fromDb = shopPurchaseRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId());
		assertThat(fromDb).hasSize(1);
	}
}
