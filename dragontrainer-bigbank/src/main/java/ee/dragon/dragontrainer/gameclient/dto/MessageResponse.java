package ee.dragon.dragontrainer.gameclient.dto;

import ee.dragon.dragontrainer.gameclient.Base64Ad;

public record MessageResponse(
		String adId,
		String message,
		String reward,
		int expiresIn,
		String probability,
		Integer encrypted) {

	public MessageResponse {
		adId = Base64Ad.open(encrypted, adId);
		message = Base64Ad.open(encrypted, message);
		probability = Base64Ad.open(encrypted, probability);
	}

	public String difficultyLabel() {
		if (probability != null && !probability.isBlank()) {
			return probability;
		}
		return message;
	}
}
