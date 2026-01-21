package ru.bell.auto_form.model.dto;

import lombok.*;

import java.time.ZonedDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerDTO {
    private String id;
    private ZonedDateTime createdAt;
    private String fio;
    private Integer age;
    private String location;
    private String telegram;
    private String email;
    private String hr;
    private String resume;
    private String questionnaire;

    @Override
    public String toString() {
        return "\"" + id + "\",\"" + createdAt + "\",\"" + fio + "\"," + age + ",\"" + location + "\",\"" + telegram
                + "\",\"" + email + "\",\"" + hr + "\",\"" + resume + "\",\"" + questionnaire + "\"";
    }
}
