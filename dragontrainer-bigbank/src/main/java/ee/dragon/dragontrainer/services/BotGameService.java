package ee.dragon.dragontrainer.services;

import ee.dragon.dragontrainer.bot.policy.BotTurnPolicy;
import ee.dragon.dragontrainer.bot.policy.BotTurnPolicy.Decision;
import ee.dragon.dragontrainer.model.GameTurn;
import ee.dragon.dragontrainer.model.ReputationCheck;
import ee.dragon.dragontrainer.model.ShopItem;
import ee.dragon.dragontrainer.model.ShopPurchase;
import ee.dragon.dragontrainer.model.Task;
import ee.dragon.dragontrainer.model.TaskDifficulty;
import ee.dragon.dragontrainer.repository.GameTurnRepository;
import ee.dragon.dragontrainer.repository.ReputationCheckRepository;
import ee.dragon.dragontrainer.repository.ShopItemRepository;
import ee.dragon.dragontrainer.repository.ShopPurchaseRepository;
import ee.dragon.dragontrainer.repository.TaskRepository;
import ee.dragon.dragontrainer.services.GameProgressService.Progress;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BotGameService {

	private final TaskService taskService;
	private final GameTurnService gameTurnService;
	private final ShopService shopService;
	private final ReputationService reputationService;
	private final GameProgressService gameProgressService;
	private final TaskRepository taskRepository;
	private final ShopItemRepository shopItemRepository;
	private final ReputationCheckRepository reputationCheckRepository;
	private final GameTurnRepository gameTurnRepository;
	private final ShopPurchaseRepository shopPurchaseRepository;
	private final BotTurnPolicy turnPolicy = new BotTurnPolicy();

	public BotGameService(
			TaskService taskService,
			GameTurnService gameTurnService,
			ShopService shopService,
			ReputationService reputationService,
			GameProgressService gameProgressService,
			TaskRepository taskRepository,
			ShopItemRepository shopItemRepository,
			ReputationCheckRepository reputationCheckRepository,
			GameTurnRepository gameTurnRepository,
			ShopPurchaseRepository shopPurchaseRepository) {
		this.taskService = taskService;
		this.gameTurnService = gameTurnService;
		this.shopService = shopService;
		this.reputationService = reputationService;
		this.gameProgressService = gameProgressService;
		this.taskRepository = taskRepository;
		this.shopItemRepository = shopItemRepository;
		this.reputationCheckRepository = reputationCheckRepository;
		this.gameTurnRepository = gameTurnRepository;
		this.shopPurchaseRepository = shopPurchaseRepository;
	}

	public void play(UUID gameSessionId) {
		BotStepResult step;
		do {
			step = step(gameSessionId);
		} while (!step.finished());
	}

	public BotStepResult step(UUID gameSessionId) {
		Progress progress = gameProgressService.current(gameSessionId);
		if (progress.lives() <= 0) {
			return stopped(progress);
		}

		List<Task> cachedTasks = reputationIsLatestAction(gameSessionId)
				? taskRepository.findByGameSession_Id(gameSessionId)
				: List.of();
		boolean continueImpossible = cachedTasks.stream().anyMatch(this::isContinuableImpossible);
		List<Task> tasks = continueImpossible
				? cachedTasks
				: taskService.fetchAndStoreTasks(gameSessionId);
		List<ShopItem> stock = continueImpossible
				? shopItemRepository.findByGameSession_Id(gameSessionId)
				: shopService.fetchAndStoreShopItems(gameSessionId);
		Decision decision = turnPolicy.decide(
				progress.lives(),
				progress.gold(),
				progress.consecutiveFailures(),
				stock,
				progress.gearPurchaseCounts(),
				tasks);
		if (decision.kind() == Decision.Kind.SKIP && continueImpossible) {
			Optional<Task> impossible = tasks.stream().filter(this::isContinuableImpossible).findFirst();
			if (impossible.isPresent()) {
				decision = Decision.solve(impossible.get());
			}
		}
		if (decision.kind() == Decision.Kind.STOP) {
			return stopped(progress);
		}
		if (decision.kind() == Decision.Kind.SKIP) {
			ReputationCheck reputation = reputationService.checkAndStoreReputation(gameSessionId);
			return new BotStepResult(
					"SKIP",
					"No normal task.",
					null,
					null,
					progress.lives(),
					progress.gold(),
					progress.level(),
					progress.score(),
					progress.highScore(),
					progress.turn(),
					reputation.getPeople(),
					reputation.getState(),
					reputation.getUnderworld(),
					false);
		}
		if (decision.kind() == Decision.Kind.SOLVE) {
			Task chosen = decision.task();
			GameTurn solved = gameTurnService.solveTask(gameSessionId, chosen.getAdId());
			return new BotStepResult(
					"SOLVE",
					chosen.getMessage() + " — " + solved.getMessage(),
					chosen.getMessage(),
					solved.isSuccess(),
					solved.getLives(),
					solved.getGold(),
					progress.level(),
					solved.getScore(),
					solved.getHighScore(),
					solved.getTurn(),
					null,
					null,
					null,
					solved.getLives() <= 0);
		}

		ShopItem item = decision.item();
		ShopPurchase purchase = shopService.buyItem(gameSessionId, item.getExternalItemId());
		boolean success = "true".equalsIgnoreCase(purchase.getShoppingSuccess());
		String action = decision.kind() == Decision.Kind.BUY_HEALING_POTION ? "BUY_HEALING_POTION" : "BUY_GEAR";
		String text = success ? "Bought " + item.getName() + "." : "Could not buy " + item.getName() + ".";
		return new BotStepResult(
				action,
				text,
				null,
				success,
				purchase.getLives(),
				purchase.getGold(),
				purchase.getLevel(),
				progress.score(),
				progress.highScore(),
				purchase.getTurn(),
				null,
				null,
				null,
				!success || purchase.getLives() <= 0);
	}

	private boolean reputationIsLatestAction(UUID gameSessionId) {
		List<ReputationCheck> checks = reputationCheckRepository.findByGameSession_IdOrderByCreatedAtAsc(gameSessionId);
		if (checks.isEmpty()) {
			return false;
		}
		Instant reputationAt = checks.get(checks.size() - 1).getCreatedAt();
		Instant latestMove = null;
		for (GameTurn turn : gameTurnRepository.findByGameSession_IdOrderByTurnAsc(gameSessionId)) {
			latestMove = later(latestMove, turn.getCreatedAt());
		}
		for (ShopPurchase purchase : shopPurchaseRepository.findByGameSession_IdOrderByCreatedAtAsc(gameSessionId)) {
			latestMove = later(latestMove, purchase.getCreatedAt());
		}
		return latestMove == null || reputationAt.isAfter(latestMove);
	}

	private boolean isContinuableImpossible(Task task) {
		if (task.getDifficulty() != TaskDifficulty.IMPOSSIBLE) {
			return false;
		}
		String message = task.getMessage() == null ? "" : task.getMessage().toLowerCase(Locale.ROOT);
		return !message.contains("steal super awesome");
	}

	private static Instant later(Instant current, Instant candidate) {
		if (candidate == null) {
			return current;
		}
		if (current == null || candidate.isAfter(current)) {
			return candidate;
		}
		return current;
	}

	private BotStepResult stopped(Progress progress) {
		return new BotStepResult(
				"STOP",
				"Game over.",
				null,
				null,
				progress.lives(),
				progress.gold(),
				progress.level(),
				progress.score(),
				progress.highScore(),
				progress.turn(),
				null,
				null,
				null,
				true);
	}
}
