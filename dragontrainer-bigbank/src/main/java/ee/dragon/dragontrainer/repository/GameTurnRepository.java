package ee.dragon.dragontrainer.repository;

import ee.dragon.dragontrainer.model.GameTurn;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameTurnRepository extends JpaRepository<GameTurn, UUID> {

	List<GameTurn> findByGameSession_IdOrderByTurnAsc(UUID gameSessionId);
}
