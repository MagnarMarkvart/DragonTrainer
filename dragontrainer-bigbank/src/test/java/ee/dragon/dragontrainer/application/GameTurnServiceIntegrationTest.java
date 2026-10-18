package ee.dragon.dragontrainer.application;

import static org.assertj.core.api.Assertions.assertThat;

import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.services.GameTurnService;
import ee.dragon.dragontrainer.model.GameTurn;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.GameTurnRepository;
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
class GameTurnServiceIntegrationTest {

	private static MockWebServer mockGameApi;

	@Autowired
	private GameTurnService gameTurnService;

	@Autowired
	private GameSessionRepository gameSessionRepository;

	@Autowired
	private GameTurnRepository gameTurnRepository;

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
	void solveTask_callsExternalApiAndPersistsGameTurn() throws InterruptedException {
		GameSession session = gameSessionRepository.save(new GameSession(
				"game-abc-123", 3, 100, 1, 0, 2500, 1));

		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "success": true,
						  "lives": 3,
						  "gold": 150,
						  "score": 10,
						  "highscore": 2500,
						  "turn": 2,
						  "message": "You successfully solved the message!"
						}
						""")
				.addHeader("Content-Type", "application/json"));

		GameTurn saved = gameTurnService.solveTask(session.getId(), "ad-1");

		RecordedRequest request = mockGameApi.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/api/v2/game-abc-123/solve/ad-1");

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getAdId()).isEqualTo("ad-1");
		assertThat(saved.isSuccess()).isTrue();
		assertThat(saved.getLives()).isEqualTo(3);
		assertThat(saved.getGold()).isEqualTo(150);
		assertThat(saved.getScore()).isEqualTo(10);
		assertThat(saved.getHighScore()).isEqualTo(2500);
		assertThat(saved.getTurn()).isEqualTo(2);
		assertThat(saved.getMessage()).isEqualTo("You successfully solved the message!");

		List<GameTurn> fromDb = gameTurnRepository.findByGameSession_IdOrderByTurnAsc(session.getId());
		assertThat(fromDb).hasSize(1);
		assertThat(fromDb.get(0).getId()).isEqualTo(saved.getId());
	}

	@Test
	@Order(2)
	void solveTask_allowsMultipleTurnsForSameSession() throws InterruptedException {
		GameSession session = gameSessionRepository.save(new GameSession(
				"game-xyz-999", 3, 100, 1, 0, 2500, 1));

		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "success": false,
						  "lives": 2,
						  "gold": 100,
						  "score": 0,
						  "highscore": 2500,
						  "turn": 2,
						  "message": "You failed!"
						}
						""")
				.addHeader("Content-Type", "application/json"));
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "success": true,
						  "lives": 2,
						  "gold": 120,
						  "score": 5,
						  "highscore": 2500,
						  "turn": 3,
						  "message": "You succeeded!"
						}
						""")
				.addHeader("Content-Type", "application/json"));

		gameTurnService.solveTask(session.getId(), "ad-a");
		gameTurnService.solveTask(session.getId(), "ad-b");

		assertThat(mockGameApi.takeRequest().getPath()).isEqualTo("/api/v2/game-xyz-999/solve/ad-a");
		assertThat(mockGameApi.takeRequest().getPath()).isEqualTo("/api/v2/game-xyz-999/solve/ad-b");

		List<GameTurn> fromDb = gameTurnRepository.findByGameSession_IdOrderByTurnAsc(session.getId());
		assertThat(fromDb).hasSize(2);
		assertThat(fromDb).extracting(GameTurn::getTurn).containsExactly(2, 3);
		assertThat(fromDb).extracting(GameTurn::getAdId).containsExactly("ad-a", "ad-b");
	}
}
