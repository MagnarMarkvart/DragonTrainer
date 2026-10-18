package ee.dragon.dragontrainer.repository;

import ee.dragon.dragontrainer.model.Task;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	List<Task> findByGameSession_Id(UUID gameSessionId);

	void deleteByGameSession_Id(UUID gameSessionId);
}
