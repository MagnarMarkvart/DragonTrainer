package ee.dragon.dragontrainer.model;

import java.util.Locale;

public enum TaskDifficulty {

	SURE_THING(1, "Sure thing"),
	WALK_IN_THE_PARK(2, "Walk in the park"),
	PIECE_OF_CAKE(3, "Piece of cake"),
	QUITE_LIKELY(4, "Quite likely"),
	GAMBLE(5, "Gamble"),
	HMM(6, "Hmmm...."),
	RATHER_DETRIMENTAL(7, "Rather detrimental"),
	RISKY(8, "Risky"),
	PLAYING_WITH_FIRE(9, "Playing with fire"),
	SUICIDE_MISSION(10, "Suicide mission"),
	IMPOSSIBLE(11, "Impossible"),
	UNKNOWN(Integer.MAX_VALUE, "");

	private final int rank;
	private final String phrase;

	TaskDifficulty(int rank, String phrase) {
		this.rank = rank;
		this.phrase = phrase;
	}

	public int rank() {
		return rank;
	}

	public String phrase() {
		return phrase;
	}

	public static TaskDifficulty fromMessage(String message) {
		if (message == null || message.isBlank()) {
			return UNKNOWN;
		}
		String normalized = message.toLowerCase(Locale.ROOT);
		for (TaskDifficulty difficulty : values()) {
			if (difficulty == UNKNOWN || difficulty.phrase.isBlank()) {
				continue;
			}
			if (normalized.contains(difficulty.phrase.toLowerCase(Locale.ROOT))) {
				return difficulty;
			}
		}
		return UNKNOWN;
	}
}
