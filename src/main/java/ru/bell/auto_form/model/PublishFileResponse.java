package ru.bell.auto_form.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishFileResponse {
    private String path;
    private String type;
    private String name;
    private Integer size;
    private String public_key;
    private String public_url;
    private String media_type;
    private String resource_id;
    // link for download
    private String file;
}
