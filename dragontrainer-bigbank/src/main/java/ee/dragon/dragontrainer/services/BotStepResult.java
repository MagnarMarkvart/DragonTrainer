package ee.dragon.dragontrainer.services;

public record BotStepResult(
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
		Integer people,
		Integer state,
		Integer underworld,
		boolean finished) {
}
