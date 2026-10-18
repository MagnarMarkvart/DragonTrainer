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
@Table(name = "reputation_checks")
public class ReputationCheck {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_session_id", nullable = false)
	private GameSession gameSession;

	private int people;
	private int state;
	private int underworld;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected ReputationCheck() {
	}

	public ReputationCheck(GameSession gameSession, int people, int state, int underworld) {
		this.gameSession = gameSession;
		this.people = people;
		this.state = state;
		this.underworld = underworld;
	}

	public UUID getId() {
		return id;
	}

	public GameSession getGameSession() {
		return gameSession;
	}

	public int getPeople() {
		return people;
	}

	public int getState() {
		return state;
	}

	public int getUnderworld() {
		return underworld;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
