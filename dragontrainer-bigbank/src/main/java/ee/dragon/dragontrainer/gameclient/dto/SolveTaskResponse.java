package ee.dragon.dragontrainer.gameclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SolveTaskResponse(
		boolean success,
		int lives,
		int gold,
		int score,
		@JsonProperty("highscore") Integer highScore,
		int turn,
		String message) {

	public int highScoreOrZero() {
		return highScore == null ? 0 : highScore;
	}
}
