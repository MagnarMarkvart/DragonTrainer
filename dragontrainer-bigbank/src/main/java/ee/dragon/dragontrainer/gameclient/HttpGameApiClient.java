package ee.dragon.dragontrainer.gameclient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ee.dragon.dragontrainer.gameclient.dto.BuyItemResponse;
import ee.dragon.dragontrainer.gameclient.dto.MessageResponse;
import ee.dragon.dragontrainer.gameclient.dto.MessagesResponse;
import ee.dragon.dragontrainer.gameclient.dto.ReputationResponse;
import ee.dragon.dragontrainer.gameclient.dto.ShopItemResponse;
import ee.dragon.dragontrainer.gameclient.dto.ShopItemsResponse;
import ee.dragon.dragontrainer.gameclient.dto.SolveTaskResponse;
import ee.dragon.dragontrainer.gameclient.dto.StartGameResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpGameApiClient implements GameApiClient {

	private final RestClient restClient;
	private final GameApiProperties properties;
	private final ObjectMapper objectMapper;

	public HttpGameApiClient(
			RestClient.Builder restClientBuilder, GameApiProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
	}

	@Override
	public StartGameResponse startNewGame() {
		return restClient.post()
				.uri(properties.startGame())
				.retrieve()
				.body(StartGameResponse.class);
	}

	@Override
	public MessagesResponse getMessages(String externalGameId) {
		JsonNode root = restClient.get()
				.uri(properties.fetchAds(), externalGameId)
				.retrieve()
				.body(JsonNode.class);
		return new MessagesResponse(GameApiJson.readList(objectMapper, root, "messages", MessageResponse.class));
	}

	@Override
	public SolveTaskResponse solveTask(String externalGameId, String adId) {
		return restClient.post()
				.uri(properties.solveTask(), externalGameId, adId)
				.retrieve()
				.body(SolveTaskResponse.class);
	}

	@Override
	public ShopItemsResponse getShopItems(String externalGameId) {
		JsonNode root = restClient.get()
				.uri(properties.openStore(), externalGameId)
				.retrieve()
				.body(JsonNode.class);
		return new ShopItemsResponse(GameApiJson.readList(objectMapper, root, "items", ShopItemResponse.class));
	}

	@Override
	public BuyItemResponse buyItem(String externalGameId, String itemId) {
		return restClient.post()
				.uri(properties.buyItem(), externalGameId, itemId)
				.retrieve()
				.body(BuyItemResponse.class);
	}

	@Override
	public ReputationResponse checkReputation(String externalGameId) {
		return restClient.post()
				.uri(properties.checkRep(), externalGameId)
				.retrieve()
				.body(ReputationResponse.class);
	}
}
