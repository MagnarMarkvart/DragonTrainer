package ee.dragon.dragontrainer.repository;

import ee.dragon.dragontrainer.model.GameSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

	Optional<GameSession> findByExternalGameId(String externalGameId);
}
