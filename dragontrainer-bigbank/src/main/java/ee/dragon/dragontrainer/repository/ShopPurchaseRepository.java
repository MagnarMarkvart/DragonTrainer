package ee.dragon.dragontrainer.repository;

import ee.dragon.dragontrainer.model.ShopPurchase;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopPurchaseRepository extends JpaRepository<ShopPurchase, UUID> {

	List<ShopPurchase> findByGameSession_IdOrderByCreatedAtAsc(UUID gameSessionId);
}
