package ee.dragon.dragontrainer.services;

import ee.dragon.dragontrainer.bot.policy.BotTurnPolicy;
import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.model.GameTurn;
import ee.dragon.dragontrainer.model.ReputationCheck;
import ee.dragon.dragontrainer.model.ShopPurchase;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.GameTurnRepository;
import ee.dragon.dragontrainer.repository.ReputationCheckRepository;
import ee.dragon.dragontrainer.repository.ShopPurchaseRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GameProgressService {

	private final GameSessionRepository gameSessionRepository;
	private final GameTurnRepository gameTurnRepository;
	private final ShopPurchaseRepository shopPurchaseRepository;
	private final ReputationCheckRepository reputationCheckRepository;

	public GameProgressService(
			GameSessionRepository gameSessionRepository,
			GameTurnRepository gameTurnRepository,
			ShopPurchaseRepository shopPurchaseRepository,
			ReputationCheckRepository reputationCheckRepository) {
		this.gameSessionRepository = gameSessionRepository;
		this.gameTurnRepository = gameTurnRepository;
		this.shopPurchaseRepository = shopPurchaseRepository;
		this.reputationCheckRepository = reputationCheckRepository;
	}

	public Progress current(UUID gameSessionId) {
		GameSession session = gameSessionRepository
				.findById(gameSessionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));

		int lives = session.getLives();
		int gold = session.getGold();
		int level = session.getLevel();
		int score = session.getScore();
		int highScore = session.getHighScore();
		int turn = session.getTurn();
		int consecutiveFailures = 0;
		Map<String, Integer> gearPurchaseCounts = new HashMap<>();

		List<Event> events = new ArrayList<>();
		for (GameTurn solved : gameTurnRepository.findByGameSession_IdOrderByTurnAsc(gameSessionId)) {
			events.add(Event.fromTurn(solved));
		}
		for (ShopPurchase purchase : shopPurchaseRepository.findByGameSession_IdOrderByCreatedAtAsc(gameSessionId)) {
			events.add(Event.fromPurchase(purchase));
		}
		events.sort(Comparator.comparingInt(Event::turn).thenComparing(Event::createdAt));

		for (Event event : events) {
			lives = event.lives();
			gold = event.gold();
			turn = event.turn();
			if (event.purchase()) {
				level = event.level();
				if (event.success() && BotTurnPolicy.GEAR_PRIORITY.contains(event.itemId())) {
					gearPurchaseCounts.merge(event.itemId(), 1, Integer::sum);
				}
				if (event.success() && BotTurnPolicy.HEALING_POTION_ID.equals(event.itemId())) {
					consecutiveFailures = 0;
				}
			} else {
				score = event.score();
				highScore = event.highScore();
				consecutiveFailures = event.success() ? 0 : consecutiveFailures + 1;
			}
		}

		List<ReputationCheck> checks = reputationCheckRepository.findByGameSession_IdOrderByCreatedAtAsc(gameSessionId);
		ReputationCheck latest = checks.isEmpty() ? null : checks.get(checks.size() - 1);
		return new Progress(
				session.getId(),
				session.getExternalGameId(),
				lives,
				gold,
				level,
				score,
				highScore,
				turn,
				consecutiveFailures,
				gearPurchaseCounts,
				latest == null ? null : latest.getPeople(),
				latest == null ? null : latest.getState(),
				latest == null ? null : latest.getUnderworld());
	}

	public record Progress(
			UUID id,
			String gameId,
			int lives,
			int gold,
			int level,
			int score,
			int highScore,
			int turn,
			int consecutiveFailures,
			Map<String, Integer> gearPurchaseCounts,
			Integer people,
			Integer state,
			Integer underworld) {
	}

	private record Event(
			int turn,
			Instant createdAt,
			boolean purchase,
			boolean success,
			int lives,
			int gold,
			int level,
			int score,
			int highScore,
			String itemId) {

		private static Event fromTurn(GameTurn solved) {
			return new Event(
					solved.getTurn(),
					solved.getCreatedAt(),
					false,
					solved.isSuccess(),
					solved.getLives(),
					solved.getGold(),
					0,
					solved.getScore(),
					solved.getHighScore(),
					null);
		}

		private static Event fromPurchase(ShopPurchase purchase) {
			return new Event(
					purchase.getTurn(),
					purchase.getCreatedAt(),
					true,
					"true".equalsIgnoreCase(purchase.getShoppingSuccess()),
					purchase.getLives(),
					purchase.getGold(),
					purchase.getLevel(),
					0,
					0,
					purchase.getExternalItemId());
		}
	}
}
