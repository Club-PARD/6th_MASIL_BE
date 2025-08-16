package pard.server.com.nadri.plan.dto.req;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum TravelTheme {
    FESTIVAL("축제", "다양한 지역 축제와 문화 행사를 즐길 수 있는 여행"),
    CLASS("원데이 클래스", "새로운 기술이나 취미를 체험하는 하루 클래스 여행"),
    NATURE("자연 경관", "자연 풍경을 감상하고 힐링하는 여행"),
    SHOPPING("쇼핑", "상점과 시장을 방문하며 쇼핑을 즐기는 여행");

    private final String label;       // 프론트에서 오는 값
    private final String description; // 설명 반환용

    public static TravelTheme fromLabel(String label) {
        return Arrays.stream(values())
                .filter(theme -> theme.getLabel().equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("잘못된 테마 값: " + label));
    }
}