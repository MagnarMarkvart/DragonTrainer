package ee.dragon.dragontrainer.config;

import ee.dragon.dragontrainer.gameclient.GameApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GameApiProperties.class)
public class GameApiConfig {
}
