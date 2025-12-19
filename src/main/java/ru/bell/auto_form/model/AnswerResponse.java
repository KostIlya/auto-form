package ru.bell.auto_form.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerResponse {
    private String id;
    private ZonedDateTime created;
    private Survey survey;
    private List<AnswerData> data;
}
