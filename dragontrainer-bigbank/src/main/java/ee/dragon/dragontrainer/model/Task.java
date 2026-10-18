package ee.dragon.dragontrainer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_session_id", nullable = false)
	private GameSession gameSession;

	@Column(nullable = false)
	private String adId;

	@Column(nullable = false, length = 2000)
	private String message;

	private int reward;

	private int expiresIn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskDifficulty difficulty;

	protected Task() {
	}

	public Task(
			GameSession gameSession,
			String adId,
			String message,
			int reward,
			int expiresIn,
			TaskDifficulty difficulty) {
		this.gameSession = gameSession;
		this.adId = adId;
		this.message = message;
		this.reward = reward;
		this.expiresIn = expiresIn;
		this.difficulty = difficulty;
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

	public String getMessage() {
		return message;
	}

	public int getReward() {
		return reward;
	}

	public int getExpiresIn() {
		return expiresIn;
	}

	public TaskDifficulty getDifficulty() {
		return difficulty;
	}
}
