package pard.server.com.nadri.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
@Getter @Setter
public class OpenAiProps {
    private String apiKey;
    private String baseUrl;
    private String model;
}
