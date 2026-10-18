package ee.dragon.dragontrainer.bot.policy;

import static org.assertj.core.api.Assertions.assertThat;

import ee.dragon.dragontrainer.bot.policy.BotTurnPolicy.Decision;
import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.model.ShopItem;
import ee.dragon.dragontrainer.model.Task;
import ee.dragon.dragontrainer.model.TaskDifficulty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BotTurnPolicyTest {

	private final GameSession session = new GameSession("g1", 3, 0, 1, 0, 0, 1);
	private final BotTurnPolicy policy = new BotTurnPolicy();

	@Test
	void decide_buysGearInPriorityOrderThenRepeats() {
		List<ShopItem> stock = gearStock();
		Map<String, Integer> counts = new HashMap<>();

		assertThat(nextGearId(3, 1500, 0, stock, counts)).isEqualTo("iron");
		counts.put("iron", 1);
		assertThat(nextGearId(3, 1200, 0, stock, counts)).isEqualTo("wingpotmax");
		counts.put("wingpotmax", 1);
		assertThat(nextGearId(3, 900, 0, stock, counts)).isEqualTo("mtrix");
		counts.put("mtrix", 1);
		assertThat(nextGearId(3, 600, 0, stock, counts)).isEqualTo("ch");
		counts.put("ch", 1);
		assertThat(nextGearId(3, 450, 0, stock, counts)).isEqualTo("rf");
		counts.put("rf", 1);
		assertThat(nextGearId(3, 450, 0, stock, counts)).isEqualTo("iron");
	}

	@Test
	void decide_doesNotSkipAnUnaffordableHigherPriorityItem() {
		List<ShopItem> stock = List.of(item("iron", 300), item("wingpotmax", 100));

		Decision decision = policy.decide(3, 150, 0, stock, Map.of(), List.of(task("easy", TaskDifficulty.SURE_THING)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("easy");
	}

	@Test
	void decide_buysHealingPotionAtTwoLivesWhenBoardHasNoEasyTasks() {
		Decision decision = policy.decide(
				2,
				80,
				0,
				List.of(item("hpot", 50), item("iron", 300)),
				Map.of(),
				List.of(task("risky", TaskDifficulty.RISKY)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.BUY_HEALING_POTION);
	}

	@Test
	void decide_solvesInsteadOfHealingWhenAnEasyTaskExists() {
		Decision decision = policy.decide(
				2,
				80,
				0,
				List.of(item("hpot", 50)),
				Map.of(),
				List.of(task("gamble", TaskDifficulty.GAMBLE), task("risky", TaskDifficulty.RISKY)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("gamble");
	}

	@Test
	void decide_buysHealingPotionAtOneLifeWhenNoSureThingExists() {
		Decision decision = policy.decide(
				1,
				80,
				0,
				List.of(item("hpot", 50)),
				Map.of(),
				List.of(task("walk", TaskDifficulty.WALK_IN_THE_PARK)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.BUY_HEALING_POTION);
	}

	@Test
	void decide_solvesSureThingAtOneLifeInsteadOfHealing() {
		Decision decision = policy.decide(
				1,
				80,
				2,
				List.of(item("hpot", 50)),
				Map.of(),
				List.of(task("sure", TaskDifficulty.SURE_THING), task("walk", TaskDifficulty.WALK_IN_THE_PARK)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("sure");
	}

	@Test
	void decide_atOneLifeBuysPotionInsteadOfAHarderTask() {
		Decision decision = policy.decide(
				1,
				300,
				3,
				List.of(item("hpot", 50), item("iron", 300)),
				Map.of(),
				List.of(task("walk", TaskDifficulty.WALK_IN_THE_PARK), task("risky", TaskDifficulty.RISKY)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.BUY_HEALING_POTION);
	}

	@Test
	void decide_atOneLifeBuysPotionWhenTheOnlySureThingIsATrap() {
		Task trap = new Task(
				session,
				"trap",
				"Steal super awesome diamond cat from Vesa Kimball",
				204,
				7,
				TaskDifficulty.SURE_THING);
		Task walk = new Task(session, "walk", "Help someone to fix their sheep", 20, 4, TaskDifficulty.WALK_IN_THE_PARK);

		Decision decision = policy.decide(1, 80, 0, List.of(item("hpot", 50)), Map.of(), List.of(trap, walk));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.BUY_HEALING_POTION);
	}

	@Test
	void decide_atTwoLivesSolvesUpToRankFive() {
		Decision decision = policy.decide(
				2,
				80,
				0,
				List.of(item("hpot", 50), item("iron", 300)),
				Map.of(),
				List.of(task("gamble", TaskDifficulty.GAMBLE), task("risky", TaskDifficulty.RISKY)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("gamble");
	}

	@Test
	void decide_buysLevelItemWhenNoRankOneToThreeAndReserveRemains() {
		Decision decision = policy.decide(
				3,
				450,
				0,
				List.of(item("iron", 300), item("hpot", 50)),
				Map.of(),
				List.of(task("gamble", TaskDifficulty.GAMBLE)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.BUY_GEAR);
		assertThat(decision.item().getExternalItemId()).isEqualTo("iron");
	}

	@Test
	void decide_doesNotBuyGearWhenItWouldSpendThePotionReserve() {
		Decision decision = policy.decide(
				2,
				449,
				0,
				List.of(item("iron", 300)),
				Map.of(),
				List.of(task("gamble", TaskDifficulty.GAMBLE)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("gamble");
	}

	@Test
	void decide_solvesRankThreeInsteadOfBuyingGear() {
		Decision decision = policy.decide(
				2,
				600,
				0,
				List.of(item("iron", 300)),
				Map.of(),
				List.of(task("cake", TaskDifficulty.PIECE_OF_CAKE)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("cake");
	}

	@Test
	void decide_afterThreeFailuresAndLowGoldPrefersTasksOneToFive() {
		Decision decision = policy.decide(
				2,
				80,
				3,
				List.of(item("hpot", 50), item("iron", 300)),
				Map.of(),
				List.of(task("risky", TaskDifficulty.RISKY), task("gamble", TaskDifficulty.GAMBLE)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("gamble");
	}

	@Test
	void decide_skipsWhenBoardHasNoNormalTask() {
		Decision decision = policy.decide(
				1,
				80,
				0,
				List.of(),
				Map.of(),
				List.of(
						task("fire", TaskDifficulty.PLAYING_WITH_FIRE),
						task("unknown", TaskDifficulty.UNKNOWN),
						task("suicide", TaskDifficulty.SUICIDE_MISSION)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SKIP);
	}

	@Test
	void decide_doesNotChooseStealSuperAwesome() {
		Task trap = new Task(
				session,
				"trap",
				"Steal super awesome diamond cat from Vesa Kimball",
				204,
				7,
				TaskDifficulty.SURE_THING);
		Task defend = new Task(
				session,
				"defend",
				"Help defending pasture from the intruders",
				131,
				7,
				TaskDifficulty.WALK_IN_THE_PARK);

		Decision decision = policy.decide(3, 0, 0, List.of(), Map.of(), List.of(trap, defend));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("defend");
	}

	@Test
	void decide_skipsWhenTheOnlyNormalTaskIsStealSuperAwesome() {
		Task trap = new Task(
				session,
				"trap",
				"Steal super awesome diamond horse from Denise Langdon",
				203,
				5,
				TaskDifficulty.SURE_THING);
		Task risky = new Task(session, "risky", "Rescue someone from a pasture", 20, 4, TaskDifficulty.RISKY);

		Decision decision = policy.decide(3, 0, 0, List.of(), Map.of(), List.of(trap, risky));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SKIP);
	}

	@Test
	void decide_triesAnImpossibleTaskWhenAHealthPotionIsUnaffordable() {
		Task trap = new Task(
				session,
				"trap",
				"Steal super awesome diamond cat from Vesa Kimball",
				500,
				7,
				TaskDifficulty.SURE_THING);
		Task impossible = new Task(
				session, "impossible", "Help defending fort from the intruders", 150, 2, TaskDifficulty.IMPOSSIBLE);

		Decision decision = policy.decide(1, 20, 0, List.of(item("hpot", 50)), Map.of(), List.of(trap, impossible));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("impossible");
	}

	@Test
	void decide_triesAnImpossibleTaskAtTwoLivesWhenBroke() {
		Task risky = new Task(session, "risky", "Rescue someone from a pasture", 20, 4, TaskDifficulty.RISKY);
		Task impossible = new Task(
				session, "impossible", "Help defending fort from the intruders", 150, 2, TaskDifficulty.IMPOSSIBLE);

		Decision decision = policy.decide(2, 20, 0, List.of(item("hpot", 50)), Map.of(), List.of(risky, impossible));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("impossible");
	}

	@Test
	void decide_picksAnImpossibleTaskWhenTheRestAreStealSuperAwesome() {
		Task trap = new Task(
				session,
				"trap",
				"Steal super awesome diamond cat from Vesa Kimball",
				500,
				7,
				TaskDifficulty.SURE_THING);
		Task laterImpossible = new Task(
				session,
				"later",
				"Help defending swamp from the intruders",
				200,
				4,
				TaskDifficulty.IMPOSSIBLE);
		Task firstImpossible = new Task(
				session,
				"first-impossible",
				"Help defending fort from the intruders",
				150,
				2,
				TaskDifficulty.IMPOSSIBLE);

		Decision decision = policy.decide(
				3, 0, 0, List.of(item("hpot", 50)), Map.of(), List.of(trap, firstImpossible, laterImpossible));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("first-impossible");
	}

	@Test
	void decide_attemptsTheFirstSureThingTrapWhenNothingElseRemains() {
		Task second = new Task(
				session,
				"second",
				"Steal super awesome diamond horse from Denise Langdon",
				900,
				4,
				TaskDifficulty.SURE_THING);
		Task first = new Task(
				session,
				"first",
				"Steal super awesome diamond cat from Vesa Kimball",
				100,
				7,
				TaskDifficulty.SURE_THING);

		Decision decision = policy.decide(3, 80, 0, List.of(item("hpot", 50)), Map.of(), List.of(first, second));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("first");
	}

	@Test
	void decide_solvesAnImpossibleTaskWhenTheBoardHasOnlyImpossibleTasks() {
		Task first = new Task(
				session, "first", "Help defending fort from the intruders", 150, 2, TaskDifficulty.IMPOSSIBLE);
		Task second = new Task(
				session, "second", "Help defending palace from the intruders", 300, 4, TaskDifficulty.IMPOSSIBLE);

		Decision decision = policy.decide(3, 200, 0, List.of(item("hpot", 50)), Map.of(), List.of(first, second));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("first");
	}

	@Test
	void decide_buysPotionAfterAFailureBeforeTheNextImpossibleTask() {
		Task impossible = new Task(
				session, "impossible", "Help defending fort from the intruders", 150, 2, TaskDifficulty.IMPOSSIBLE);

		Decision decision = policy.decide(2, 80, 1, List.of(item("hpot", 50)), Map.of(), List.of(impossible));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.BUY_HEALING_POTION);
	}

	@Test
	void decide_topsUpLivesBeforeAnImpossibleTaskWhenTrapsFillTheBoard() {
		Task trap = new Task(
				session,
				"trap",
				"Steal super awesome diamond cat from Vesa Kimball",
				500,
				7,
				TaskDifficulty.SURE_THING);
		Task impossible = new Task(
				session, "impossible", "Help defending fort from the intruders", 150, 2, TaskDifficulty.IMPOSSIBLE);

		Decision decision = policy.decide(2, 80, 0, List.of(item("hpot", 50)), Map.of(), List.of(trap, impossible));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("impossible");
	}

	@Test
	void decide_topsUpLivesBeforeAttemptingASureThingTrap() {
		Task trap = new Task(
				session,
				"trap",
				"Steal super awesome diamond cat from Vesa Kimball",
				500,
				7,
				TaskDifficulty.SURE_THING);

		Decision decision = policy.decide(2, 80, 1, List.of(item("hpot", 50)), Map.of(), List.of(trap));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.BUY_HEALING_POTION);
	}

	@Test
	void decide_stillSolvesAnOrdinaryStealWhenItIsTheOnlyNormalTask() {
		Task steal = new Task(
				session,
				"steal",
				"Steal pan delivery to Nelida Collins and share some of the profits with the people.",
				48,
				6,
				TaskDifficulty.PIECE_OF_CAKE);
		Task risky = new Task(session, "risky", "Rescue someone from a pasture", 20, 4, TaskDifficulty.RISKY);

		Decision decision = policy.decide(3, 0, 0, List.of(), Map.of(), List.of(steal, risky));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("steal");
	}

	@Test
	void decide_prefersADefenseMissionOverAStealAtTheSameRank() {
		Task steal = new Task(session, "steal", "Steal beer from the people", 90, 4, TaskDifficulty.WALK_IN_THE_PARK);
		Task defend = new Task(
				session, "defend", "Help defending plains from the intruders", 40, 4, TaskDifficulty.WALK_IN_THE_PARK);

		Decision decision = policy.decide(3, 0, 0, List.of(), Map.of(), List.of(steal, defend));

		assertThat(decision.task().getAdId()).isEqualTo("defend");
	}

	@Test
	void decide_solvesEasiestNormalTaskWhenAHarderOneIsAlsoPresent() {
		Decision decision = policy.decide(
				3,
				300,
				3,
				List.of(),
				Map.of(),
				List.of(task("sure", TaskDifficulty.SURE_THING), task("hmm", TaskDifficulty.HMM)));

		assertThat(decision.kind()).isEqualTo(Decision.Kind.SOLVE);
		assertThat(decision.task().getAdId()).isEqualTo("sure");
	}

	private String nextGearId(
			int lives, int gold, int failures, List<ShopItem> stock, Map<String, Integer> counts) {
		Decision decision = policy.decide(lives, gold, failures, stock, counts, List.of());
		assertThat(decision.kind()).isEqualTo(Decision.Kind.BUY_GEAR);
		return decision.item().getExternalItemId();
	}

	private List<ShopItem> gearStock() {
		return List.of(
				item("rf", 300),
				item("ch", 300),
				item("mtrix", 300),
				item("wingpotmax", 300),
				item("iron", 300),
				item("hpot", 50));
	}

	private ShopItem item(String id, int cost) {
		return new ShopItem(session, id, id, cost);
	}

	private Task task(String adId, TaskDifficulty difficulty) {
		return new Task(session, adId, adId, 10, 4, difficulty);
	}
}
