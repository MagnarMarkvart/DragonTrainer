package ee.dragon.dragontrainer.application;

import static org.assertj.core.api.Assertions.assertThat;

import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.services.GameSessionService;
import ee.dragon.dragontrainer.services.GameTurnService;
import ee.dragon.dragontrainer.services.ReputationService;
import ee.dragon.dragontrainer.services.ShopService;
import ee.dragon.dragontrainer.services.TaskService;
import ee.dragon.dragontrainer.model.GameTurn;
import ee.dragon.dragontrainer.model.ReputationCheck;
import ee.dragon.dragontrainer.model.ShopItem;
import ee.dragon.dragontrainer.model.ShopPurchase;
import ee.dragon.dragontrainer.model.Task;
import java.io.IOException;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GamePlaythroughIntegrationTest {

	private static MockWebServer mockGameApi;

	@Autowired
	private GameSessionService gameSessionService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private GameTurnService gameTurnService;

	@Autowired
	private ShopService shopService;

	@Autowired
	private ReputationService reputationService;

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
	void fullPlaythrough_canCallAllGameOperationsInSequence() {
		enqueueStartGame();
		enqueueMessages();
		enqueueSolve();
		enqueueShop();
		enqueueBuy();
		enqueueReputation();

		GameSession session = gameSessionService.startNewGame();
		assertThat(session.getExternalGameId()).isEqualTo("play-game-1");

		List<Task> tasks = taskService.fetchAndStoreTasks(session.getId());
		assertThat(tasks).hasSize(1);

		GameTurn turn = gameTurnService.solveTask(session.getId(), "ad-1");
		assertThat(turn.isSuccess()).isTrue();

		List<ShopItem> shopItems = shopService.fetchAndStoreShopItems(session.getId());
		assertThat(shopItems).hasSize(1);

		ShopPurchase purchase = shopService.buyItem(session.getId(), "hpot");
		assertThat(purchase.getLives()).isEqualTo(4);

		ReputationCheck reputation = reputationService.checkAndStoreReputation(session.getId());
		assertThat(reputation.getPeople()).isEqualTo(7);

		assertThat(mockGameApi.getRequestCount()).isEqualTo(6);
	}

	private static void enqueueStartGame() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "gameId": "play-game-1",
						  "lives": 3,
						  "gold": 100,
						  "level": 1,
						  "score": 0,
						  "highScore": 1000,
						  "turn": 1
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueMessages() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "messages": [
						    {
						      "adId": "ad-1",
						      "message": "Easy task. Piece of cake!",
						      "reward": "25",
						      "expiresIn": 2
						    }
						  ]
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueSolve() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "success": true,
						  "lives": 3,
						  "gold": 125,
						  "score": 5,
						  "highscore": 1000,
						  "turn": 2,
						  "message": "Success"
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueShop() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "items": [
						    { "id": "hpot", "name": "Healing potion", "cost": 50 }
						  ]
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueBuy() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "shoppingSuccess": "true",
						  "gold": 75,
						  "lives": 4,
						  "level": 1,
						  "turn": 3
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueReputation() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "people": 7,
						  "state": 0,
						  "underworld": 2
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}
}
