package ee.dragon.dragontrainer.gameclient;

import ee.dragon.dragontrainer.gameclient.dto.BuyItemResponse;
import ee.dragon.dragontrainer.gameclient.dto.MessagesResponse;
import ee.dragon.dragontrainer.gameclient.dto.ReputationResponse;
import ee.dragon.dragontrainer.gameclient.dto.ShopItemsResponse;
import ee.dragon.dragontrainer.gameclient.dto.SolveTaskResponse;
import ee.dragon.dragontrainer.gameclient.dto.StartGameResponse;

public interface GameApiClient {

	StartGameResponse startNewGame();

	MessagesResponse getMessages(String externalGameId);

	SolveTaskResponse solveTask(String externalGameId, String adId);

	ShopItemsResponse getShopItems(String externalGameId);

	BuyItemResponse buyItem(String externalGameId, String itemId);

	ReputationResponse checkReputation(String externalGameId);
}
