package com.cinescore.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActorSearchItemDTO {

    @JsonAlias("id")
    private Long id;

    @JsonAlias("name")
    private String name;

    @JsonAlias("known_for_department")
    private String knownForDepartment;

    @JsonAlias("profile_path")
    private String profilePath;

    @JsonAlias("known_for")
    private List<KnownForDTO> knownFor;

    private String photo;
    private String knownForTitles;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KnownForDTO {
        @JsonAlias("title")
        private String title;

        @JsonAlias("name")
        private String name;
    }
}
