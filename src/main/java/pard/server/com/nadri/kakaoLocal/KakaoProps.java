package pard.server.com.nadri.kakaoLocal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.kakao")
@Getter @Setter
public class KakaoProps {
    private String baseUrl;
    private String restApiKey;
}
