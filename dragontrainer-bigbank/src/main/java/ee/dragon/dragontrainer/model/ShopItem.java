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
import java.util.UUID;

@Entity
@Table(name = "shop_items")
public class ShopItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_session_id", nullable = false)
	private GameSession gameSession;

	@Column(nullable = false)
	private String externalItemId;

	@Column(nullable = false)
	private String name;

	private int cost;

	protected ShopItem() {
	}

	public ShopItem(GameSession gameSession, String externalItemId, String name, int cost) {
		this.gameSession = gameSession;
		this.externalItemId = externalItemId;
		this.name = name;
		this.cost = cost;
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

	public String getName() {
		return name;
	}

	public int getCost() {
		return cost;
	}
}
