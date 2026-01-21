package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bell.auto_form.model.dto.AnswerDTO;
import ru.bell.auto_form.model.AnswerData;
import ru.bell.auto_form.model.AnswerFileValue;
import ru.bell.auto_form.model.AnswerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@Slf4j
public class JsonService {
    private final ObjectMapper objectMapper;

    public JsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Для поддержки ZonedDateTime
    }

    public AnswerDTO parseJsonToAnswer(String jsonString) {
        try {
            AnswerResponse answerResponse = objectMapper.readValue(jsonString, AnswerResponse.class);
            return convertToAnswer(answerResponse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AnswerDTO convertToAnswer(AnswerResponse answerResponse) {
        AnswerDTO answerDTO = new AnswerDTO();
        answerDTO.setId(answerResponse.getId());
        answerDTO.setCreatedAt(answerResponse.getCreated());

        if (answerResponse.getData() != null) {
            for (AnswerData answerData : answerResponse.getData()) {
                setAnswerField(answerDTO, answerData);
            }
        }

        return answerDTO;
    }

    private void setAnswerField(AnswerDTO answerDTO, AnswerData answerData) {
        String fieldId = answerData.getId();
        JsonNode valueNode = answerData.getValue();

        if (valueNode == null || valueNode.isNull()) {
            return;
        }

        try {
            switch (fieldId) {
                case "fio":
                    answerDTO.setFio(valueNode.asText());
                    break;
                case "age":
                    if (valueNode.isInt()) {
                        answerDTO.setAge(valueNode.asInt());
                    }
                    break;
                case "location":
                    answerDTO.setLocation(valueNode.asText());
                    break;
                case "telegram":
                    answerDTO.setTelegram(valueNode.asText());
                    break;
                case "mail":
                    answerDTO.setEmail(valueNode.asText());
                    break;
                case "hr":
                    answerDTO.setHr(valueNode.asText());
                    break;
                case "rezumes":
                    if (valueNode.isArray() && !valueNode.isEmpty()) {
                        AnswerFileValue fileValue = objectMapper.convertValue(valueNode.get(0), AnswerFileValue.class);

                        String fileUrl = "https://forms.yandex.ru/u/files?path=" + fileValue.getPath();
                        answerDTO.setResume(fileUrl);
                    }
                    break;
                case "questionnaires":
                    if (valueNode.isArray() && !valueNode.isEmpty()) {
                        AnswerFileValue fileValue = objectMapper.convertValue(valueNode.get(0), AnswerFileValue.class);

                        String fileUrl = "https://forms.yandex.ru/u/files?path=" + fileValue.getPath();
                        answerDTO.setQuestionnaire(fileUrl);
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to parse field {}: {}", fieldId, e.getMessage());
        }
    }
}
