package ee.dragon.dragontrainer.services;

import ee.dragon.dragontrainer.gameclient.GameApiClient;
import ee.dragon.dragontrainer.gameclient.dto.ReputationResponse;
import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.model.ReputationCheck;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.ReputationCheckRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReputationService {

	private final GameApiClient gameApiClient;
	private final GameSessionRepository gameSessionRepository;
	private final ReputationCheckRepository reputationCheckRepository;

	public ReputationService(
			GameApiClient gameApiClient,
			GameSessionRepository gameSessionRepository,
			ReputationCheckRepository reputationCheckRepository) {
		this.gameApiClient = gameApiClient;
		this.gameSessionRepository = gameSessionRepository;
		this.reputationCheckRepository = reputationCheckRepository;
	}

	@Transactional
	public ReputationCheck checkAndStoreReputation(UUID gameSessionId) {
		GameSession session = gameSessionRepository
				.findById(gameSessionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));

		ReputationResponse response = gameApiClient.checkReputation(session.getExternalGameId());
		ReputationCheck check = new ReputationCheck(
				session, response.people(), response.state(), response.underworld());
		return reputationCheckRepository.save(check);
	}
}
