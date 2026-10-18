package ee.dragon.dragontrainer.bot;

import static org.assertj.core.api.Assertions.assertThat;

import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.services.BotGameService;
import ee.dragon.dragontrainer.model.ShopPurchase;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.GameTurnRepository;
import ee.dragon.dragontrainer.repository.ReputationCheckRepository;
import ee.dragon.dragontrainer.repository.ShopPurchaseRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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
class BotGameServiceIntegrationTest {

	private static MockWebServer mockGameApi;

	@Autowired
	private BotGameService botGameService;

	@Autowired
	private GameSessionRepository gameSessionRepository;

	@Autowired
	private GameTurnRepository gameTurnRepository;

	@Autowired
	private ShopPurchaseRepository shopPurchaseRepository;

	@Autowired
	private ReputationCheckRepository reputationCheckRepository;

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
	void play_buysIronBeforeOtherGearThenSolves() {
		GameSession session = gameSessionRepository.save(new GameSession(
				"bot-game-1", 3, 500, 1, 0, 0, 1));

		enqueueMessages("ad-gamble", "Gamble", "ad-risky", "Risky");
		enqueuePriorityShop();
		enqueuePurchase(true, 3, 200, 2);
		enqueueMessages("ad-easy", "Sure thing!", "ad-hard", "Gamble!");
		enqueuePriorityShop();
		enqueueSolve("ad-easy", false, 0, 2, 200);

		botGameService.play(session.getId());

		assertThat(shopPurchaseRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId()))
				.extracting(ShopPurchase::getExternalItemId)
				.containsExactly("iron");
		assertThat(gameTurnRepository.findByGameSession_IdOrderByTurnAsc(session.getId()))
				.extracting(turn -> turn.getAdId())
				.containsExactly("ad-easy");
	}

	@Test
	void play_buysHealingPotionOnlyWhenNoEasyTaskRemains() {
		GameSession session = gameSessionRepository.save(new GameSession(
				"bot-game-2", 2, 80, 1, 0, 0, 1));

		enqueueMessages("ad-risky", "Risky", "ad-fire", "Playing with fire");
		enqueuePriorityShop();
		enqueuePurchase(true, 3, 30);
		enqueueMessages("ad-easy", "Sure thing!", "ad-hard", "Gamble!");
		enqueuePriorityShop();
		enqueueSolve("ad-easy", false, 0, 2, 30);

		botGameService.play(session.getId());

		assertThat(shopPurchaseRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId()))
				.extracting(ShopPurchase::getExternalItemId)
				.containsExactly("hpot");
		assertThat(reputationCheckRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId()))
				.isEmpty();
	}

	@Test
	void play_atOneLifeBuysHealingPotionInsteadOfAHarderTask() {
		GameSession session = gameSessionRepository.save(new GameSession(
				"bot-game-3", 1, 80, 1, 0, 0, 1));

		enqueueMessages("ad-walk", "Walk in the park", "ad-cake", "Piece of cake");
		enqueuePriorityShop();
		enqueuePurchase(true, 2, 30);
		enqueueMessages("ad-easy", "Sure thing!", "ad-hard", "Gamble!");
		enqueuePriorityShop();
		enqueueSolve("ad-easy", false, 0, 2, 30);

		botGameService.play(session.getId());

		assertThat(shopPurchaseRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId()))
				.extracting(ShopPurchase::getExternalItemId)
				.containsExactly("hpot");
		assertThat(gameTurnRepository.findByGameSession_IdOrderByTurnAsc(session.getId()))
				.extracting(turn -> turn.getAdId())
				.containsExactly("ad-easy");
		assertThat(reputationCheckRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId()))
				.isEmpty();
	}

	@Test
	void play_checksReputationOnceThenSolvesImpossibleWithoutAnotherFetch() {
		GameSession session = gameSessionRepository.save(new GameSession(
				"bot-game-impossible", 3, 200, 1, 0, 0, 1));

		enqueueTwoTasks(
				"ad-risky",
				"Guard the gate",
				"Risky",
				80,
				"ad-impossible",
				"Hold the bridge alone",
				"Impossible",
				400);
		enqueuePriorityShop();
		enqueueReputation();
		enqueueSolve("ad-impossible", false, 0, 2, 200);

		botGameService.play(session.getId());

		assertThat(reputationCheckRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId()))
				.hasSize(1);
		assertThat(gameTurnRepository.findByGameSession_IdOrderByTurnAsc(session.getId()))
				.extracting(turn -> turn.getAdId())
				.containsExactly("ad-impossible");
	}

	@Test
	void play_skipsWhenBoardHasNoNormalTask() {
		GameSession session = gameSessionRepository.save(new GameSession(
				"bot-game-4", 3, 40, 1, 0, 0, 1));

		enqueueMessages("ad-fire", "Playing with fire", "ad-risky", "Risky");
		enqueuePriorityShop();
		enqueueReputation();
		enqueueMessages("ad-easy", "Sure thing!", "ad-hard", "Gamble!");
		enqueuePriorityShop();
		enqueueSolve("ad-easy", false, 0, 2, 40);

		botGameService.play(session.getId());

		assertThat(reputationCheckRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId()))
				.hasSize(1);
		assertThat(gameTurnRepository.findByGameSession_IdOrderByTurnAsc(session.getId()))
				.extracting(turn -> turn.getAdId())
				.containsExactly("ad-easy");
	}

	@Test
	void play_skipsStealSuperAwesomeAndSolvesTheOtherNormalTask() {
		GameSession session = gameSessionRepository.save(new GameSession(
				"bot-game-5", 3, 40, 1, 0, 0, 1));

		enqueueTwoTasks(
				"ad-trap",
				"Steal super awesome diamond cat from Vesa Kimball",
				"Sure thing!",
				204,
				"ad-defend",
				"Help defending pasture from the intruders",
				"Walk in the park",
				131);
		enqueuePriorityShop();
		enqueueSolve("ad-defend", false, 0, 2, 40);

		botGameService.play(session.getId());

		assertThat(gameTurnRepository.findByGameSession_IdOrderByTurnAsc(session.getId()))
				.extracting(turn -> turn.getAdId())
				.containsExactly("ad-defend");
		assertThat(reputationCheckRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId()))
				.isEmpty();
	}

	@Test
	void play_solvesBase64SureThingWithTheDecodedAdId() throws InterruptedException {
		GameSession session = gameSessionRepository.save(new GameSession(
				"bot-b64", 3, 0, 0, 0, 0, 0));

		String encodedAdId = Base64.getEncoder().encodeToString("SQmuFkbf".getBytes(StandardCharsets.UTF_8));
		String encodedMessage = Base64.getEncoder().encodeToString(
				"Rescue the plain quest.".getBytes(StandardCharsets.UTF_8));
		String encodedProbability = Base64.getEncoder().encodeToString("Sure thing".getBytes(StandardCharsets.UTF_8));
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "messages": [
						    {
						      "adId": "%s",
						      "message": "%s",
						      "reward": 40,
						      "expiresIn": 4,
						      "encrypted": 1,
						      "probability": "%s"
						    }
						  ]
						}
						"""
						.formatted(encodedAdId, encodedMessage, encodedProbability))
				.addHeader("Content-Type", "application/json"));
		enqueuePriorityShop();
		enqueueSolve("SQmuFkbf", true, 0, 1, 40);

		int alreadyRecorded = mockGameApi.getRequestCount();
		botGameService.play(session.getId());
		for (int i = 0; i < alreadyRecorded; i++) {
			mockGameApi.takeRequest();
		}

		assertThat(mockGameApi.takeRequest().getPath()).isEqualTo("/api/v2/bot-b64/messages");
		assertThat(mockGameApi.takeRequest().getPath()).isEqualTo("/api/v2/bot-b64/shop");
		RecordedRequest solve = mockGameApi.takeRequest();
		assertThat(solve.getMethod()).isEqualTo("POST");
		assertThat(solve.getPath()).isEqualTo("/api/v2/bot-b64/solve/SQmuFkbf");
		assertThat(gameTurnRepository.findByGameSession_IdOrderByTurnAsc(session.getId()))
				.extracting(turn -> turn.getAdId())
				.containsExactly("SQmuFkbf");
	}

	private static void enqueuePriorityShop() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "items": [
						    { "id": "hpot", "name": "Healing potion", "cost": 50 },
						    { "id": "rf", "name": "Rocket Fuel", "cost": 300 },
						    { "id": "wingpotmax", "name": "Potion of Awesome Wings", "cost": 300 },
						    { "id": "iron", "name": "Iron Plating", "cost": 300 }
						  ]
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueuePurchase(boolean success, int lives, int gold) {
		enqueuePurchase(success, lives, gold, 1);
	}

	private static void enqueuePurchase(boolean success, int lives, int gold, int level) {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "shoppingSuccess": "%s",
						  "gold": %d,
						  "lives": %d,
						  "level": %d,
						  "turn": 3
						}
						"""
						.formatted(success, gold, lives, level))
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueReputation() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "people": 1,
						  "state": 0,
						  "underworld": 2
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueTwoTasks(
			String firstAdId,
			String firstText,
			String firstProbability,
			int firstReward,
			String secondAdId,
			String secondText,
			String secondProbability,
			int secondReward) {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "messages": [
						    {
						      "adId": "%s",
						      "message": "%s",
						      "reward": "%d",
						      "expiresIn": 7,
						      "probability": "%s"
						    },
						    {
						      "adId": "%s",
						      "message": "%s",
						      "reward": "%d",
						      "expiresIn": 7,
						      "probability": "%s"
						    }
						  ]
						}
						"""
						.formatted(
								firstAdId,
								firstText,
								firstReward,
								firstProbability,
								secondAdId,
								secondText,
								secondReward,
								secondProbability))
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueMessages(String firstAdId, String firstMessage, String secondAdId, String secondMessage) {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "messages": [
						    {
						      "adId": "%s",
						      "message": "Hard quest",
						      "reward": "100",
						      "expiresIn": 2,
						      "probability": "%s"
						    },
						    {
						      "adId": "%s",
						      "message": "Easy quest",
						      "reward": "10",
						      "expiresIn": 4,
						      "probability": "%s"
						    }
						  ]
						}
						"""
						.formatted(firstAdId, firstMessage, secondAdId, secondMessage))
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueSolve(String adId, boolean success, int lives, int turn, int gold) {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "success": %s,
						  "lives": %d,
						  "gold": %d,
						  "score": 5,
						  "highscore": 1000,
						  "turn": %d,
						  "message": "Solved %s"
						}
						"""
						.formatted(success, lives, gold, turn, adId))
				.addHeader("Content-Type", "application/json"));
	}
}
