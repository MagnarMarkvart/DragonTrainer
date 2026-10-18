package ee.dragon.dragontrainer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_sessions")
public class GameSession {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String externalGameId;

	private int lives;
	private int gold;
	private int level;
	private int score;
	private int highScore;
	private int turn;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected GameSession() {
	}

	public GameSession(
			String externalGameId,
			int lives,
			int gold,
			int level,
			int score,
			int highScore,
			int turn) {
		this.externalGameId = externalGameId;
		this.lives = lives;
		this.gold = gold;
		this.level = level;
		this.score = score;
		this.highScore = highScore;
		this.turn = turn;
	}

	public UUID getId() {
		return id;
	}

	public String getExternalGameId() {
		return externalGameId;
	}

	public int getLives() {
		return lives;
	}

	public int getGold() {
		return gold;
	}

	public int getLevel() {
		return level;
	}

	public int getScore() {
		return score;
	}

	public int getHighScore() {
		return highScore;
	}

	public int getTurn() {
		return turn;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
