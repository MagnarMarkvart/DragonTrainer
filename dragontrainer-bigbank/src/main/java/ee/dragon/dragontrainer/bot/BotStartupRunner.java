package ee.dragon.dragontrainer.bot;

import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.services.BotGameService;
import ee.dragon.dragontrainer.services.GameSessionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "game.bot", name = "enabled", havingValue = "true")
public class BotStartupRunner implements ApplicationRunner {

	private final GameSessionService gameSessionService;
	private final BotGameService botGameService;

	public BotStartupRunner(GameSessionService gameSessionService, BotGameService botGameService) {
		this.gameSessionService = gameSessionService;
		this.botGameService = botGameService;
	}

	@Override
	public void run(ApplicationArguments args) {
		GameSession session = gameSessionService.startNewGame();
		botGameService.play(session.getId());
	}
}
