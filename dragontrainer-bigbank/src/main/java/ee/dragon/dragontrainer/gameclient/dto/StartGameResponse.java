package ee.dragon.dragontrainer.gameclient.dto;

public record StartGameResponse(
		String gameId,
		int lives,
		int gold,
		int level,
		int score,
		int highScore,
		int turn) {
}
