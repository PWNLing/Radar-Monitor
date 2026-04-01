package com.radar.monitor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RadarData {
    private String type;
    private Double timestamp;
    private Header header;
    @JsonProperty("ego_position")
    private Position egoPosition;
    @JsonProperty("target_count")
    private Integer targetCount;
    private List<Track> tracks;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private Long sequence;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Position {
        private Double latitude;
        private Double longitude;
        private Double altitude;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Track {
        @JsonProperty("track_id")
        private Integer trackId;
        private String type;
        private Position position;
        private Double distance;
        private Double velocity;
    }
}