package ee.dragon.dragontrainer.services;

import ee.dragon.dragontrainer.gameclient.GameApiClient;
import ee.dragon.dragontrainer.gameclient.dto.MessageResponse;
import ee.dragon.dragontrainer.gameclient.dto.MessagesResponse;
import ee.dragon.dragontrainer.model.GameSession;
import ee.dragon.dragontrainer.model.Task;
import ee.dragon.dragontrainer.model.TaskDifficulty;
import ee.dragon.dragontrainer.repository.GameSessionRepository;
import ee.dragon.dragontrainer.repository.TaskRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class TaskService {

	private final GameApiClient gameApiClient;
	private final GameSessionRepository gameSessionRepository;
	private final TaskRepository taskRepository;

	public TaskService(
			GameApiClient gameApiClient,
			GameSessionRepository gameSessionRepository,
			TaskRepository taskRepository) {
		this.gameApiClient = gameApiClient;
		this.gameSessionRepository = gameSessionRepository;
		this.taskRepository = taskRepository;
	}

	@Transactional
	public List<Task> fetchAndStoreTasks(UUID gameSessionId) {
		GameSession session = gameSessionRepository
				.findById(gameSessionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found"));

		MessagesResponse response = gameApiClient.getMessages(session.getExternalGameId());
		taskRepository.deleteByGameSession_Id(gameSessionId);

		List<Task> tasks = response.messages().stream()
				.map(message -> toTask(session, message))
				.toList();
		return taskRepository.saveAll(tasks);
	}

	private Task toTask(GameSession session, MessageResponse message) {
		TaskDifficulty difficulty = TaskDifficulty.fromMessage(message.difficultyLabel());
		return new Task(
				session,
				message.adId(),
				message.message(),
				Integer.parseInt(message.reward()),
				message.expiresIn(),
				difficulty);
	}
}
