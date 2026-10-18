package ee.dragon.dragontrainer.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

public final class GameResponses {

	private GameResponses() {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record GameStateResponse(
			UUID id,
			String gameId,
			int lives,
			int gold,
			int level,
			int score,
			int highScore,
			int turn,
			ReputationResponse reputation) {
	}

	public record ReputationResponse(int people, int state, int underworld) {
	}

	public record TaskResponse(String adId, String message, int reward, int expiresIn, String difficulty) {
	}

	public record ShopItemResponse(String id, String name, int cost) {
	}

	public record SolveResponse(
			String adId,
			String message,
			boolean success,
			int lives,
			int gold,
			int score,
			int highScore,
			int turn) {
	}

	public record PurchaseResponse(String itemId, boolean success, int lives, int gold, int level, int turn) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record BotStepResponse(
			String action,
			String text,
			String taskMessage,
			Boolean success,
			int lives,
			int gold,
			int level,
			int score,
			int highScore,
			int turn,
			ReputationResponse reputation,
			boolean finished) {
	}
}
