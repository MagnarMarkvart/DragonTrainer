package ee.dragon.dragontrainer.gameclient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.List;

final class GameApiJson {

	private GameApiJson() {
	}

	static <T> List<T> readList(ObjectMapper objectMapper, JsonNode root, String wrappedProperty, Class<T> elementType) {
		JsonNode arrayNode = root.isArray() ? root : root.get(wrappedProperty);
		if (arrayNode == null || !arrayNode.isArray()) {
			return List.of();
		}
		return objectMapper.convertValue(
				arrayNode,
				objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
	}
}
