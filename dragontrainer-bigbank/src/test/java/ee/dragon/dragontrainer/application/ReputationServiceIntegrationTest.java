package ee.dragon.dragontrainer.application;

import static org.assertj.core.api.Assertions.assertThat;

import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.services.ReputationService;
import ee.dragon.dragontrainer.model.ReputationCheck;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.ReputationCheckRepository;
import java.io.IOException;
import java.util.List;
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
class ReputationServiceIntegrationTest {

	private static MockWebServer mockGameApi;

	@Autowired
	private ReputationService reputationService;

	@Autowired
	private GameSessionRepository gameSessionRepository;

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
	void checkAndStoreReputation_callsExternalApiAndPersistsResult() throws InterruptedException {
		GameSession session = gameSessionRepository.save(new GameSession(
				"game-rep-1", 3, 100, 1, 0, 2500, 1));

		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "people": 10,
						  "state": -5,
						  "underworld": 3
						}
						""")
				.addHeader("Content-Type", "application/json"));

		ReputationCheck saved = reputationService.checkAndStoreReputation(session.getId());

		RecordedRequest request = mockGameApi.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/api/v2/game-rep-1/investigate/reputation");

		assertThat(saved.getPeople()).isEqualTo(10);
		assertThat(saved.getState()).isEqualTo(-5);
		assertThat(saved.getUnderworld()).isEqualTo(3);

		List<ReputationCheck> fromDb =
				reputationCheckRepository.findByGameSession_IdOrderByCreatedAtAsc(session.getId());
		assertThat(fromDb).hasSize(1);
	}
}
