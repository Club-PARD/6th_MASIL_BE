package pard.server.com.nadri.kakaoLocal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.swing.text.Document;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoordinateRecord(List<Document> documents) {
    public record Document(String x, String y){}
}
