package ee.dragon.dragontrainer.gameclient;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "game.api")
public record GameApiProperties(
		@NotBlank String baseUrl,
		@NotBlank String startGame,
		@NotBlank String fetchAds,
		@NotBlank String solveTask,
		@NotBlank String openStore,
		@NotBlank String buyItem,
		@NotBlank String checkRep) {
}
