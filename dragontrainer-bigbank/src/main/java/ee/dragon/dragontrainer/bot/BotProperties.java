package ee.dragon.dragontrainer.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game.bot")
public record BotProperties(boolean enabled) {
}
