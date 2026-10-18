package ee.dragon.dragontrainer.bot.policy;

import ee.dragon.dragontrainer.model.ShopItem;
import ee.dragon.dragontrainer.model.Task;
import ee.dragon.dragontrainer.model.TaskDifficulty;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class BotTurnPolicy {

	public static final List<String> GEAR_PRIORITY = List.of("iron", "wingpotmax", "mtrix", "ch", "rf");
	public static final int ONE_LIFE_MAX_RANK = 1;
	public static final int NORMAL_TASK_MAX_RANK = 5;
	public static final int GEAR_TASK_MAX_RANK = 3;
	public static final int POTION_GOLD_RESERVE = 150;
	public static final int FAILURES_BEFORE_GEAR_ONLY = 3;
	public static final int MAX_LIVES = 3;
	public static final String HEALING_POTION_ID = "hpot";

	public Decision decide(
			int lives,
			int gold,
			int consecutiveFailures,
			List<ShopItem> stock,
			Map<String, Integer> gearPurchaseCounts,
			List<Task> tasks) {
		if (lives <= 0) {
			return Decision.stop();
		}

		if (lives == 1) {
			Optional<Task> sureThing = bestInBand(tasks, 1, ONE_LIFE_MAX_RANK);
			if (sureThing.isPresent()) {
				return Decision.solve(sureThing.get());
			}
			Optional<ShopItem> potion = affordablePotion(stock, gold);
			if (potion.isPresent()) {
				return Decision.buyPotion(potion.get());
			}
		} else {
			Optional<Decision> forced = forcedBoardDecision(lives, gold, consecutiveFailures, stock, tasks);
			if (forced.isPresent()) {
				return forced.get();
			}

			if (!hasPlayableRank(tasks, 1, GEAR_TASK_MAX_RANK)) {
				Optional<ShopItem> gear = nextGear(stock, gearPurchaseCounts, gold);
				if (gear.isPresent()) {
					return Decision.buyGear(gear.get());
				}
			}

			Optional<Task> task = bestInBand(tasks, 1, NORMAL_TASK_MAX_RANK);
			if (task.isPresent()) {
				return Decision.solve(task.get());
			}

			if (lives == 2 && consecutiveFailures < FAILURES_BEFORE_GEAR_ONLY) {
				Optional<ShopItem> potion = affordablePotion(stock, gold);
				if (potion.isPresent()) {
					return Decision.buyPotion(potion.get());
				}
			}
		}

		Optional<Decision> forced = forcedBoardDecision(lives, gold, consecutiveFailures, stock, tasks);
		if (forced.isPresent()) {
			return forced.get();
		}
		if (affordablePotion(stock, gold).isEmpty()) {
			Optional<Task> impossible = firstImpossible(tasks);
			if (impossible.isPresent()) {
				return Decision.solve(impossible.get());
			}
		}
		return Decision.skip();
	}

	private Optional<Decision> forcedBoardDecision(
			int lives,
			int gold,
			int consecutiveFailures,
			List<ShopItem> stock,
			List<Task> tasks) {
		if (!isOnlyImpossibleOrTrap(tasks)) {
			return Optional.empty();
		}
		if (consecutiveFailures >= 1 && lives < MAX_LIVES) {
			Optional<ShopItem> potion = affordablePotion(stock, gold);
			if (potion.isPresent()) {
				return Optional.of(Decision.buyPotion(potion.get()));
			}
		}
		Optional<Task> impossible = firstImpossible(tasks);
		if (impossible.isPresent()) {
			return Optional.of(Decision.solve(impossible.get()));
		}
		return firstSuperAwesomeSteal(tasks).map(Decision::solve);
	}

	private boolean isOnlyImpossibleOrTrap(List<Task> tasks) {
		if (tasks.isEmpty()) {
			return false;
		}
		for (Task task : tasks) {
			if (isSuperAwesomeSteal(task)) {
				continue;
			}
			if (task.getDifficulty() != TaskDifficulty.IMPOSSIBLE) {
				return false;
			}
		}
		return true;
	}

	private Optional<Task> firstImpossible(List<Task> tasks) {
		for (Task task : tasks) {
			if (!isSuperAwesomeSteal(task) && task.getDifficulty() == TaskDifficulty.IMPOSSIBLE) {
				return Optional.of(task);
			}
		}
		return Optional.empty();
	}

	private Optional<Task> firstSuperAwesomeSteal(List<Task> tasks) {
		Task fallback = null;
		for (Task task : tasks) {
			if (!isSuperAwesomeSteal(task)) {
				continue;
			}
			if (task.getDifficulty().rank() == ONE_LIFE_MAX_RANK) {
				return Optional.of(task);
			}
			if (fallback == null) {
				fallback = task;
			}
		}
		return Optional.ofNullable(fallback);
	}

	private Optional<Task> bestInBand(List<Task> tasks, int minRank, int maxRank) {
		List<Task> easiest = new ArrayList<>();
		int bestRank = Integer.MAX_VALUE;
		for (Task task : tasks) {
			if (isSuperAwesomeSteal(task)) {
				continue;
			}
			int rank = task.getDifficulty().rank();
			if (rank < minRank || rank > maxRank) {
				continue;
			}
			if (rank < bestRank) {
				easiest.clear();
				bestRank = rank;
			}
			if (rank == bestRank) {
				easiest.add(task);
			}
		}
		Optional<Task> withoutSteal = easiest.stream()
				.filter(task -> !isSteal(task))
				.max(Comparator.comparingInt(Task::getReward));
		if (withoutSteal.isPresent()) {
			return withoutSteal;
		}
		return easiest.stream().max(Comparator.comparingInt(Task::getReward));
	}

	private boolean isSteal(Task task) {
		return task.getMessage() != null && task.getMessage().toLowerCase(Locale.ROOT).startsWith("steal");
	}

	private boolean isSuperAwesomeSteal(Task task) {
		return task.getMessage() != null
				&& task.getMessage().toLowerCase(Locale.ROOT).contains("steal super awesome");
	}

	private Optional<ShopItem> affordablePotion(List<ShopItem> stock, int gold) {
		return find(stock, HEALING_POTION_ID).filter(potion -> gold >= potion.getCost());
	}

	private boolean hasPlayableRank(List<Task> tasks, int minRank, int maxRank) {
		return tasks.stream().anyMatch(task -> {
			if (isSuperAwesomeSteal(task)) {
				return false;
			}
			int rank = task.getDifficulty().rank();
			return rank >= minRank && rank <= maxRank;
		});
	}

	private Optional<ShopItem> nextGear(List<ShopItem> stock, Map<String, Integer> gearPurchaseCounts, int gold) {
		int fewestBuys = GEAR_PRIORITY.stream()
				.mapToInt(itemId -> gearPurchaseCounts.getOrDefault(itemId, 0))
				.min()
				.orElse(0);
		for (String itemId : GEAR_PRIORITY) {
			if (gearPurchaseCounts.getOrDefault(itemId, 0) != fewestBuys) {
				continue;
			}
			Optional<ShopItem> item = find(stock, itemId);
			if (item.isEmpty()) {
				continue;
			}
			if (item.get().getCost() + POTION_GOLD_RESERVE > gold) {
				return Optional.empty();
			}
			return item;
		}
		return Optional.empty();
	}

	private Optional<ShopItem> find(List<ShopItem> stock, String itemId) {
		return stock.stream()
				.filter(item -> itemId.equals(item.getExternalItemId()))
				.findFirst();
	}

	public record Decision(Kind kind, ShopItem item, Task task) {

		public enum Kind {
			BUY_HEALING_POTION,
			BUY_GEAR,
			SOLVE,
			SKIP,
			STOP
		}

		public static Decision buyPotion(ShopItem item) {
			return new Decision(Kind.BUY_HEALING_POTION, item, null);
		}

		public static Decision buyGear(ShopItem item) {
			return new Decision(Kind.BUY_GEAR, item, null);
		}

		public static Decision solve(Task task) {
			return new Decision(Kind.SOLVE, null, task);
		}

		public static Decision skip() {
			return new Decision(Kind.SKIP, null, null);
		}

		public static Decision stop() {
			return new Decision(Kind.STOP, null, null);
		}
	}
}
