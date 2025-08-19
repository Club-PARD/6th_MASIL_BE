package pard.server.com.nadri.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 프론트 주소를 정확히 지정 (쿠키/세션/자격증명 쓰면 * 금지)
                .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://169.213.137.35:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 토큰/Location 등 응답 헤더를 프론트에서 읽게 하려면 노출
                .exposedHeaders("Authorization", "Location")
                // 세션/쿠키(JSESSIONID)나 axios withCredentials:true 쓸 때 반드시 true
                .allowCredentials(true)
                // 프리플라이트 캐시 (초)
                .maxAge(3600);
    }
}
