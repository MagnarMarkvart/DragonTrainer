package ee.dragon.dragontrainer.services;

import ee.dragon.dragontrainer.gameclient.GameApiClient;
import ee.dragon.dragontrainer.gameclient.dto.SolveTaskResponse;
import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.model.GameTurn;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.GameTurnRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GameTurnService {

	private final GameApiClient gameApiClient;
	private final GameSessionRepository gameSessionRepository;
	private final GameTurnRepository gameTurnRepository;

	public GameTurnService(
			GameApiClient gameApiClient,
			GameSessionRepository gameSessionRepository,
			GameTurnRepository gameTurnRepository) {
		this.gameApiClient = gameApiClient;
		this.gameSessionRepository = gameSessionRepository;
		this.gameTurnRepository = gameTurnRepository;
	}

	@Transactional
	public GameTurn solveTask(UUID gameSessionId, String adId) {
		GameSession session = gameSessionRepository
				.findById(gameSessionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));

		SolveTaskResponse response = gameApiClient.solveTask(session.getExternalGameId(), adId);
		GameTurn gameTurn = new GameTurn(
				session,
				adId,
				response.success(),
				response.lives(),
				response.gold(),
				response.score(),
				response.highScoreOrZero(),
				response.turn(),
				response.message());
		return gameTurnRepository.save(gameTurn);
	}
}
