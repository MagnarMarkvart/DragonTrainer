package ee.dragon.dragontrainer.application;

import static org.assertj.core.api.Assertions.assertThat;

import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.services.GameSessionService;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import java.io.IOException;
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
class GameSessionServiceIntegrationTest {

	private static MockWebServer mockGameApi;

	@Autowired
	private GameSessionService gameSessionService;

	@Autowired
	private GameSessionRepository gameSessionRepository;

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
	void startNewGame_callsExternalApiAndPersistsGameState() throws InterruptedException {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "gameId": "game-abc-123",
						  "lives": 3,
						  "gold": 100,
						  "level": 1,
						  "score": 0,
						  "highScore": 2500,
						  "turn": 1
						}
						""")
				.addHeader("Content-Type", "application/json"));

		GameSession saved = gameSessionService.startNewGame();

		RecordedRequest request = mockGameApi.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/api/v2/game/start");

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getExternalGameId()).isEqualTo("game-abc-123");
		assertThat(saved.getLives()).isEqualTo(3);
		assertThat(saved.getGold()).isEqualTo(100);
		assertThat(saved.getLevel()).isEqualTo(1);
		assertThat(saved.getScore()).isEqualTo(0);
		assertThat(saved.getHighScore()).isEqualTo(2500);
		assertThat(saved.getTurn()).isEqualTo(1);

		GameSession fromDb = gameSessionRepository.findByExternalGameId("game-abc-123").orElseThrow();
		assertThat(fromDb.getId()).isEqualTo(saved.getId());
		assertThat(fromDb.getGold()).isEqualTo(100);
		assertThat(fromDb.getHighScore()).isEqualTo(2500);
	}
}
