package pard.server.com.nadri.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import pard.server.com.nadri.kakaoLocal.KakaoProps;

@Configuration
public class KakaoLocalConfig {
    @Bean("kakaoLocalWebClient")
    WebClient kakaoLocalWebClient(KakaoProps p){
        return WebClient.builder()
                .baseUrl(p.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + p.getRestApiKey())
                .build();
    }
}
