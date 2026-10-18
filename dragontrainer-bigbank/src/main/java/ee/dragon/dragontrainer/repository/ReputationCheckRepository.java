package ee.dragon.dragontrainer.repository;

import ee.dragon.dragontrainer.model.ReputationCheck;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReputationCheckRepository extends JpaRepository<ReputationCheck, UUID> {

	List<ReputationCheck> findByGameSession_IdOrderByCreatedAtAsc(UUID gameSessionId);
}
