package pard.server.com.nadri.kakaoLocal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pard.server.com.nadri.kakaoLocal.dto.CoordinateRecord;
import pard.server.com.nadri.openai.service.OpenAiService;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;

@Service
public class KakaoLocalService {
    @Qualifier("kakaoLocalWebClient")
    private final WebClient kakaoClient;
    private final OpenAiService openAiService;

    public KakaoLocalService(
            @Qualifier("kakaoLocalWebClient") WebClient kakaoClient,
            OpenAiService openAiService
    ) {
        this.kakaoClient = kakaoClient;
        this.openAiService = openAiService;
    }

    public record Coord(String x, String y) {}

    public void convertToCoordinate(CreatePlanDto createPlanDto){
        Coord coord = kakaoClient.get()
                .uri(u -> u.path("/v2/local/search/address.json")
                        .queryParam("query", createPlanDto.getOrigin())
                        .queryParam("size", 1)
                        .build())
                .retrieve()
                .bodyToMono(CoordinateRecord.class)
                .map(res -> {
                    var d = res.documents().get(0);
                    return new Coord(d.x(), d.y()); // x=경도, y=위도 (문자열)
                })
                .block();
        assert coord != null;
        System.out.println(coord.x + coord.y);
        searchByCategory(coord.x(), coord.y(), createPlanDto);
    }

    public void searchByCategory(String x, String y, CreatePlanDto createPlanDto){

    }
}
