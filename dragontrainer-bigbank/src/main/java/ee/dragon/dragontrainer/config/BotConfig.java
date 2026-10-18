package ee.dragon.dragontrainer.config;

import ee.dragon.dragontrainer.bot.BotProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BotProperties.class)
public class BotConfig {
}
