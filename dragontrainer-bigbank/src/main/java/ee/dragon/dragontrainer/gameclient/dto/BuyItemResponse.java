package ee.dragon.dragontrainer.gameclient.dto;

public record BuyItemResponse(String shoppingSuccess, int gold, int lives, int level, int turn) {
}
