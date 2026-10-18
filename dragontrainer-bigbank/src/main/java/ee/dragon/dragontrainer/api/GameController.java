package ee.dragon.dragontrainer.api;

import ee.dragon.dragontrainer.api.GameResponses.BotStepResponse;
import ee.dragon.dragontrainer.api.GameResponses.GameStateResponse;
import ee.dragon.dragontrainer.api.GameResponses.PurchaseResponse;
import ee.dragon.dragontrainer.api.GameResponses.ReputationResponse;
import ee.dragon.dragontrainer.api.GameResponses.ShopItemResponse;
import ee.dragon.dragontrainer.api.GameResponses.SolveResponse;
import ee.dragon.dragontrainer.api.GameResponses.TaskResponse;
import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.model.GameTurn;
import ee.dragon.dragontrainer.model.ReputationCheck;
import ee.dragon.dragontrainer.model.ShopItem;
import ee.dragon.dragontrainer.model.ShopPurchase;
import ee.dragon.dragontrainer.model.Task;
import ee.dragon.dragontrainer.services.BotGameService;
import ee.dragon.dragontrainer.services.BotStepResult;
import ee.dragon.dragontrainer.services.GameProgressService;
import ee.dragon.dragontrainer.services.GameProgressService.Progress;
import ee.dragon.dragontrainer.services.GameSessionService;
import ee.dragon.dragontrainer.services.GameTurnService;
import ee.dragon.dragontrainer.services.ReputationService;
import ee.dragon.dragontrainer.services.ShopService;
import ee.dragon.dragontrainer.services.TaskService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

	private final GameSessionService gameSessionService;
	private final GameProgressService gameProgressService;
	private final TaskService taskService;
	private final GameTurnService gameTurnService;
	private final ShopService shopService;
	private final ReputationService reputationService;
	private final BotGameService botGameService;

	public GameController(
			GameSessionService gameSessionService,
			GameProgressService gameProgressService,
			TaskService taskService,
			GameTurnService gameTurnService,
			ShopService shopService,
			ReputationService reputationService,
			BotGameService botGameService) {
		this.gameSessionService = gameSessionService;
		this.gameProgressService = gameProgressService;
		this.taskService = taskService;
		this.gameTurnService = gameTurnService;
		this.shopService = shopService;
		this.reputationService = reputationService;
		this.botGameService = botGameService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public GameStateResponse startGame() {
		GameSession session = gameSessionService.startNewGame();
		return state(gameProgressService.current(session.getId()));
	}

	@GetMapping("/{id}")
	public GameStateResponse game(@PathVariable UUID id) {
		return state(gameProgressService.current(id));
	}

	@GetMapping("/{id}/tasks")
	public List<TaskResponse> tasks(@PathVariable UUID id) {
		return taskService.fetchAndStoreTasks(id).stream().map(this::task).toList();
	}

	@PostMapping("/{id}/tasks/{adId}/solve")
	public SolveResponse solve(@PathVariable UUID id, @PathVariable String adId) {
		GameTurn solved = gameTurnService.solveTask(id, adId);
		return new SolveResponse(
				solved.getAdId(),
				solved.getMessage(),
				solved.isSuccess(),
				solved.getLives(),
				solved.getGold(),
				solved.getScore(),
				solved.getHighScore(),
				solved.getTurn());
	}

	@GetMapping("/{id}/shop")
	public List<ShopItemResponse> shop(@PathVariable UUID id) {
		return shopService.fetchAndStoreShopItems(id).stream().map(this::shopItem).toList();
	}

	@PostMapping("/{id}/shop/{itemId}")
	public PurchaseResponse buy(@PathVariable UUID id, @PathVariable String itemId) {
		ShopPurchase purchase = shopService.buyItem(id, itemId);
		return new PurchaseResponse(
				purchase.getExternalItemId(),
				"true".equalsIgnoreCase(purchase.getShoppingSuccess()),
				purchase.getLives(),
				purchase.getGold(),
				purchase.getLevel(),
				purchase.getTurn());
	}

	@PostMapping("/{id}/reputation")
	public ReputationResponse reputation(@PathVariable UUID id) {
		return reputation(reputationService.checkAndStoreReputation(id));
	}

	@PostMapping("/{id}/bot/step")
	public BotStepResponse botStep(@PathVariable UUID id) {
		return botStep(botGameService.step(id));
	}

	private GameStateResponse state(Progress progress) {
		return new GameStateResponse(
				progress.id(),
				progress.gameId(),
				progress.lives(),
				progress.gold(),
				progress.level(),
				progress.score(),
				progress.highScore(),
				progress.turn(),
				progress.people() == null
						? null
						: new ReputationResponse(progress.people(), progress.state(), progress.underworld()));
	}

	private TaskResponse task(Task task) {
		return new TaskResponse(
				task.getAdId(), task.getMessage(), task.getReward(), task.getExpiresIn(), task.getDifficulty().name());
	}

	private ShopItemResponse shopItem(ShopItem item) {
		return new ShopItemResponse(item.getExternalItemId(), item.getName(), item.getCost());
	}

	private ReputationResponse reputation(ReputationCheck check) {
		return new ReputationResponse(check.getPeople(), check.getState(), check.getUnderworld());
	}

	private BotStepResponse botStep(BotStepResult step) {
		ReputationResponse reputation = step.people() == null
				? null
				: new ReputationResponse(step.people(), step.state(), step.underworld());
		return new BotStepResponse(
				step.action(),
				step.text(),
				step.taskMessage(),
				step.success(),
				step.lives(),
				step.gold(),
				step.level(),
				step.score(),
				step.highScore(),
				step.turn(),
				reputation,
				step.finished());
	}
}
