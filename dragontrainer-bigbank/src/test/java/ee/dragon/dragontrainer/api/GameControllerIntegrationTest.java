package ee.dragon.dragontrainer.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GameControllerIntegrationTest {

	private static MockWebServer mockGameApi;

	@Autowired
	private MockMvc mockMvc;

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
	void botStep_returnsTurnTextAndReputationWhenSkipping() throws Exception {
		enqueueStart();
		enqueueMessages("ad-easy", "Sure thing!", "Clean the bucket");
		enqueueShop();
		enqueueSolve(true, 3, 40, 40, 2, "You successfully solved the mission!");
		enqueueMessages("ad-fire", "Playing with fire", "Hard quest");
		enqueueShop();
		enqueueReputation(4, -2, 0);

		MvcResult started = mockMvc.perform(post("/api/games")).andExpect(status().isCreated()).andReturn();
		String id = JsonPath.read(started.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/games/{id}/bot/step", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.action").value("SOLVE"))
				.andExpect(jsonPath("$.text").value("Clean the bucket — You successfully solved the mission!"))
				.andExpect(jsonPath("$.score").value(40))
				.andExpect(jsonPath("$.gold").value(40))
				.andExpect(jsonPath("$.reputation").doesNotExist())
				.andExpect(jsonPath("$.finished").value(false));

		mockMvc.perform(post("/api/games/{id}/bot/step", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.action").value("SKIP"))
				.andExpect(jsonPath("$.text").value("No normal task."))
				.andExpect(jsonPath("$.score").value(40))
				.andExpect(jsonPath("$.gold").value(40))
				.andExpect(jsonPath("$.reputation.people").value(4))
				.andExpect(jsonPath("$.reputation.state").value(-2))
				.andExpect(jsonPath("$.reputation.underworld").value(0))
				.andExpect(jsonPath("$.finished").value(false));
	}

	@Test
	void manualPlay_listsTasksSolvesBuysAndChecksReputation() throws Exception {
		enqueueStart();
		enqueueMessages("ad-1", "Piece of cake", "Easy task. Piece of cake!");
		enqueueSolve(true, 3, 125, 25, 2, "Success");
		enqueueShop();
		enqueueBuy();
		enqueueReputation(7, 0, 1);

		MvcResult started = mockMvc.perform(post("/api/games"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.gameId").value("api-game"))
				.andExpect(jsonPath("$.lives").value(3))
				.andExpect(jsonPath("$.gold").value(100))
				.andReturn();
		String id = JsonPath.read(started.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/games/{id}/tasks", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].adId").value("ad-1"))
				.andExpect(jsonPath("$[0].message").value("Easy task. Piece of cake!"))
				.andExpect(jsonPath("$[0].difficulty").value("PIECE_OF_CAKE"));

		mockMvc.perform(post("/api/games/{id}/tasks/{adId}/solve", id, "ad-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Success"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.gold").value(125))
				.andExpect(jsonPath("$.score").value(25));

		mockMvc.perform(get("/api/games/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gold").value(125))
				.andExpect(jsonPath("$.score").value(25))
				.andExpect(jsonPath("$.reputation").doesNotExist());

		mockMvc.perform(get("/api/games/{id}/shop", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("hpot"))
				.andExpect(jsonPath("$[0].cost").value(50));

		mockMvc.perform(post("/api/games/{id}/shop/{itemId}", id, "hpot").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itemId").value("hpot"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.lives").value(4))
				.andExpect(jsonPath("$.gold").value(75));

		mockMvc.perform(post("/api/games/{id}/reputation", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.people").value(7))
				.andExpect(jsonPath("$.state").value(0))
				.andExpect(jsonPath("$.underworld").value(1));

		mockMvc.perform(get("/api/games/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lives").value(4))
				.andExpect(jsonPath("$.gold").value(75))
				.andExpect(jsonPath("$.score").value(25))
				.andExpect(jsonPath("$.reputation.people").value(7));
	}

	private static void enqueueStart() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "gameId": "api-game",
						  "lives": 3,
						  "gold": 100,
						  "level": 0,
						  "score": 0,
						  "highScore": 0,
						  "turn": 0
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueMessages(String adId, String probability, String message) {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "messages": [
						    {
						      "adId": "%s",
						      "message": "%s",
						      "reward": "40",
						      "expiresIn": 5,
						      "probability": "%s"
						    }
						  ]
						}
						"""
						.formatted(adId, message, probability))
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

	private static void enqueueSolve(boolean success, int lives, int gold, int score, int turn, String message) {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "success": %s,
						  "lives": %d,
						  "gold": %d,
						  "score": %d,
						  "highscore": 1000,
						  "turn": %d,
						  "message": "%s"
						}
						"""
						.formatted(success, lives, gold, score, turn, message))
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueBuy() {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "shoppingSuccess": "true",
						  "gold": 75,
						  "lives": 4,
						  "level": 0,
						  "turn": 3
						}
						""")
				.addHeader("Content-Type", "application/json"));
	}

	private static void enqueueReputation(int people, int state, int underworld) {
		mockGameApi.enqueue(new MockResponse()
				.setBody("""
						{
						  "people": %d,
						  "state": %d,
						  "underworld": %d
						}
						"""
						.formatted(people, state, underworld))
				.addHeader("Content-Type", "application/json"));
	}
}
