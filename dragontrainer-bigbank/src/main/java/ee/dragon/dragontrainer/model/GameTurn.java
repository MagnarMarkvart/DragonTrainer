package ee.dragon.dragontrainer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_turns")
public class GameTurn {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_session_id", nullable = false)
	private GameSession gameSession;

	@Column(nullable = false)
	private String adId;

	private boolean success;
	private int lives;
	private int gold;
	private int score;
	private int highScore;
	private int turn;

	@Column(nullable = false, length = 2000)
	private String message;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected GameTurn() {
	}

	public GameTurn(
			GameSession gameSession,
			String adId,
			boolean success,
			int lives,
			int gold,
			int score,
			int highScore,
			int turn,
			String message) {
		this.gameSession = gameSession;
		this.adId = adId;
		this.success = success;
		this.lives = lives;
		this.gold = gold;
		this.score = score;
		this.highScore = highScore;
		this.turn = turn;
		this.message = message;
	}

	public UUID getId() {
		return id;
	}

	public GameSession getGameSession() {
		return gameSession;
	}

	public String getAdId() {
		return adId;
	}

	public boolean isSuccess() {
		return success;
	}

	public int getLives() {
		return lives;
	}

	public int getGold() {
		return gold;
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

	public String getMessage() {
		return message;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
