package ee.dragon.dragontrainer.repository;

import ee.dragon.dragontrainer.model.ShopItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopItemRepository extends JpaRepository<ShopItem, UUID> {

	List<ShopItem> findByGameSession_Id(UUID gameSessionId);

	void deleteByGameSession_Id(UUID gameSessionId);
}
