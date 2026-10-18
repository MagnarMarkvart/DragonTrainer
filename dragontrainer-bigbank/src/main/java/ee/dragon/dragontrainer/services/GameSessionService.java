package ee.dragon.dragontrainer.services;

import ee.dragon.dragontrainer.gameclient.GameApiClient;
import ee.dragon.dragontrainer.gameclient.dto.StartGameResponse;
import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameSessionService {

	private final GameApiClient gameApiClient;
	private final GameSessionRepository gameSessionRepository;

	public GameSessionService(GameApiClient gameApiClient, GameSessionRepository gameSessionRepository) {
		this.gameApiClient = gameApiClient;
		this.gameSessionRepository = gameSessionRepository;
	}

	@Transactional
	public GameSession startNewGame() {
		StartGameResponse response = gameApiClient.startNewGame();
		GameSession entity = new GameSession(
				response.gameId(),
				response.lives(),
				response.gold(),
				response.level(),
				response.score(),
				response.highScore(),
				response.turn());
		return gameSessionRepository.save(entity);
	}
}
