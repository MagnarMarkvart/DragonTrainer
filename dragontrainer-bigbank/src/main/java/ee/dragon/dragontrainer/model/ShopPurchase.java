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
@Table(name = "shop_purchases")
public class ShopPurchase {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_session_id", nullable = false)
	private GameSession gameSession;

	@Column(nullable = false)
	private String externalItemId;

	@Column(nullable = false)
	private String shoppingSuccess;

	private int gold;
	private int lives;
	private int level;
	private int turn;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected ShopPurchase() {
	}

	public ShopPurchase(
			GameSession gameSession,
			String externalItemId,
			String shoppingSuccess,
			int gold,
			int lives,
			int level,
			int turn) {
		this.gameSession = gameSession;
		this.externalItemId = externalItemId;
		this.shoppingSuccess = shoppingSuccess;
		this.gold = gold;
		this.lives = lives;
		this.level = level;
		this.turn = turn;
	}

	public UUID getId() {
		return id;
	}

	public GameSession getGameSession() {
		return gameSession;
	}

	public String getExternalItemId() {
		return externalItemId;
	}

	public String getShoppingSuccess() {
		return shoppingSuccess;
	}

	public int getGold() {
		return gold;
	}

	public int getLives() {
		return lives;
	}

	public int getLevel() {
		return level;
	}

	public int getTurn() {
		return turn;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
