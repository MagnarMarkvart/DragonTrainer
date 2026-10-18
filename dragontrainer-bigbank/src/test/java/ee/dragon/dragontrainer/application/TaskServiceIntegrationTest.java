package ee.dragon.dragontrainer.application;

import static org.assertj.core.api.Assertions.assertThat;

import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.services.TaskService;
import ee.dragon.dragontrainer.model.Task;
import ee.dragon.dragontrainer.model.TaskDifficulty;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.TaskRepository;
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
class TaskServiceIntegrationTest {

	private static MockWebServer mockGameApi;

	@Autowired
	private TaskService taskService;

	@Autowired
	private GameSessionRepository gameSessionRepository;

	@Autowired
	private TaskRepository taskRepository;

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
	void fetchAndStoreTasks_callsExternalApiAndPersistsTasksWithDifficulty() throws InterruptedException {
		GameSession session = gameSessionRepository.save(new GameSession(
				"game-abc-123", 3, 100, 1, 0, 2500, 1));

		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "messages": [
						    {
						      "adId": "ad-1",
						      "message": "Help a friend. Piece of cake!",
						      "reward": "50",
						      "expiresIn": 3
						    },
						    {
						      "adId": "ad-2",
						      "message": "Steal dragon eggs. Gamble!",
						      "reward": "200",
						      "expiresIn": 1
						    }
						  ]
						}
						""")
				.addHeader("Content-Type", "application/json"));

		List<Task> saved = taskService.fetchAndStoreTasks(session.getId());

		RecordedRequest request = mockGameApi.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/api/v2/game-abc-123/messages");

		assertThat(saved).hasSize(2);

		Task easyTask = saved.stream()
				.filter(task -> task.getAdId().equals("ad-1"))
				.findFirst()
				.orElseThrow();
		assertThat(easyTask.getReward()).isEqualTo(50);
		assertThat(easyTask.getExpiresIn()).isEqualTo(3);
		assertThat(easyTask.getDifficulty()).isEqualTo(TaskDifficulty.PIECE_OF_CAKE);

		Task gambleTask = saved.stream()
				.filter(task -> task.getAdId().equals("ad-2"))
				.findFirst()
				.orElseThrow();
		assertThat(gambleTask.getDifficulty()).isEqualTo(TaskDifficulty.GAMBLE);

		List<Task> fromDb = taskRepository.findByGameSession_Id(session.getId());
		assertThat(fromDb).hasSize(2);
		assertThat(fromDb).extracting(Task::getAdId).containsExactlyInAnyOrder("ad-1", "ad-2");
	}

	@Test
	void fetchAndStoreTasks_storesBase64AdsAsPlainTextAndDifficulty() throws InterruptedException {
		GameSession session = gameSessionRepository.save(new GameSession(
				"game-encoded", 3, 100, 1, 0, 2500, 1));

		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "messages": [
						    {
						      "adId": "U1FtdUZrYmY=",
						      "message": "SW5maWx0cmF0ZSBUaGUgSmFja2FscyBhbmQgcmVjb3ZlciB0aGVpciBzZWNyZXRzLg==",
						      "reward": 126,
						      "expiresIn": 2,
						      "encrypted": 1,
						      "probability": "Umlza3k="
						    }
						  ]
						}
						""")
				.addHeader("Content-Type", "application/json"));

		List<Task> saved = taskService.fetchAndStoreTasks(session.getId());

		assertThat(saved).hasSize(1);
		assertThat(saved.getFirst().getAdId()).isEqualTo("SQmuFkbf");
		assertThat(saved.getFirst().getMessage()).isEqualTo("Infiltrate The Jackals and recover their secrets.");
		assertThat(saved.getFirst().getReward()).isEqualTo(126);
		assertThat(saved.getFirst().getDifficulty()).isEqualTo(TaskDifficulty.RISKY);
	}

	@Test
	void fetchAndStoreTasks_decodesTheLiveArrayShape() throws InterruptedException {
		GameSession session = gameSessionRepository.save(new GameSession(
				"game-live-shape", 3, 100, 1, 0, 2500, 1));

		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						[
						  {
						    "adId": "NTZuVVNsdkI=",
						    "message": "SW52ZXN0aWdhdGUgT2xlZ8OhcmlvIEFpbWFyIGFuZCBmaW5kIG91dCB0aGVpciByZWxhdGlvbiB0byB0aGUgbWFnaWMgc3F1aXJyZWwu",
						    "reward": 130,
						    "expiresIn": 2,
						    "encrypted": 1,
						    "probability": "SG1tbS4uLi4="
						  }
						]
						""")
				.addHeader("Content-Type", "application/json"));

		List<Task> saved = taskService.fetchAndStoreTasks(session.getId());

		assertThat(saved).hasSize(1);
		assertThat(saved.getFirst().getAdId()).isEqualTo("56nUSlvB");
		assertThat(saved.getFirst().getMessage())
				.isEqualTo("Investigate Olegário Aimar and find out their relation to the magic squirrel.");
		assertThat(saved.getFirst().getDifficulty()).isEqualTo(TaskDifficulty.HMM);
	}
}
