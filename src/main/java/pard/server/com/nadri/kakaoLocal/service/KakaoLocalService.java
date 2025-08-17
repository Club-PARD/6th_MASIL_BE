package pard.server.com.nadri.kakaoLocal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pard.server.com.nadri.kakaoLocal.dto.Coord;
import pard.server.com.nadri.kakaoLocal.dto.CoordinateRecord;
import pard.server.com.nadri.kakaoLocal.dto.KakaoPlaceDto;
import pard.server.com.nadri.kakaoLocal.dto.PlaceDto;
import pard.server.com.nadri.openai.service.OpenAiService;
import pard.server.com.nadri.plan.dto.req.CreatePlanDto;

import java.net.URI;
import java.util.List;

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

    public Coord convertToCoordinate(String origin){
        Coord coord = kakaoClient.get()
                .uri(u -> u.path("/v2/local/search/address.json")
                        .queryParam("query", origin)
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

        return coord;
    }

    public List<PlaceDto> searchByCategory(Coord coord, String code){
        KakaoPlaceDto kakaoPlaceDto = kakaoClient.get()
                .uri(u -> u.path("/v2/local/search/category.json")
                        .queryParam("category_group_code", code)
                        .queryParam("x", coord.x())
                        .queryParam("y", coord.y())
                        .queryParam("radius", 20000)
                        .build())
                .retrieve()
                .bodyToMono(KakaoPlaceDto.class)
                .block();

        assert kakaoPlaceDto != null;
        return kakaoPlaceDto.getDocuments();
    }

    public List<PlaceDto> searchByCategories(Coord coord, List<String> codes) {
        return codes.stream()
                .flatMap(code -> searchByCategory(coord, code).stream())
                .distinct()
                .toList();
    }
}
