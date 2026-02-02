package ru.bell.auto_form.service;

import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.model.ExportRequest;
import ru.bell.auto_form.model.YandexToken;
import ru.bell.auto_form.model.dto.AnswerDTO;
import ru.bell.auto_form.model.record.ResultExport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class FormService {
    private final YandexConfigProperties yandexConfigProperties;
    private final JsonService jsonService;
    @Autowired
    private YandexToken yandexToken;
    private final String BASE_URL;

    private final RestTemplate restTemplate;

    public FormService(RestTemplate restTemplate, JsonService jsonService, YandexConfigProperties yandexConfigProperties) {
        this.restTemplate = restTemplate;
        this.jsonService = jsonService;
        this.yandexConfigProperties = yandexConfigProperties;
        this.BASE_URL = "https://api.forms.yandex.net/v1/surveys/"
                + yandexConfigProperties.getSurveyId()
                + "/answers";
    }

    public List<AnswerDTO> getAnswersInLastSeconds(Integer seconds) {
        final String url = BASE_URL + "/export";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + yandexToken.getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);

            ZonedDateTime endTime = ZonedDateTime.now(ZoneOffset.UTC);
            ZonedDateTime startTime = endTime.minusSeconds(seconds);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

            ExportRequest request = new ExportRequest();
            request.setColumns(getColumnsWithId());
            request.setFormat("csv");
            request.setUpload("default");
            request.setUpload_files(false);
            request.setStarted_at(startTime.format(formatter));
            request.setFinished_at(endTime.format(formatter));

            ObjectMapper mapper = new ObjectMapper();
            String requestBodyJson = mapper.writeValueAsString(request);

            log.debug("getAnswersInLastSeconds(): yandexToken: {}", yandexToken);

            log.debug("getAnswersInLastSeconds(): uriBuilder: {}", uriBuilder.toUriString());
            log.debug("getAnswersInLastSeconds(): headers: {}", headers);
            log.debug("getAnswersInLastSeconds(): requestBodyJson: {}", requestBodyJson);
            HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);
            log.debug("getAnswersInLastSeconds(): entity: {}", entity);

            ResponseEntity<ResultExport> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.POST,
                    entity,
                    ResultExport.class
            );

            if (!response.getStatusCode().equals(HttpStatusCode.valueOf(202))) {
                throw new RuntimeException("getAnswersInLastSeconds: Don't get export");
            }
            // response.getBody().status(): ok, fail, wait, not_running

            if (response.getBody().status().equals("not_running") || response.getBody().status().equals("fail")) {
                throw new RuntimeException("getAnswersInLastSeconds: Status export is " + response.getBody().status());
            }

            return getAnswers(response.getBody().id());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to export answers", e);
        }
    }

    private List<String> getColumnsWithId() {
        List<String> columns = new LinkedList<>();
        columns.add("id");
        return columns;
    }

    private List<AnswerDTO> getAnswers(String id) {
        final String url = BASE_URL +  "/export-results";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "OAuth " + yandexToken.getAccessToken());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url).queryParam("task_id", id);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class
        );
        String responseBody = response.getBody();
        if (response.getStatusCode().equals(HttpStatusCode.valueOf(202))) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);
            String status = jsonNode.get("status").asText();
            while (status.equals("wait")) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e.getMessage());
                }
                response = restTemplate.exchange(
                        uriBuilder.toUriString(),
                        HttpMethod.GET,
                        entity,
                        String.class
                );
                if (response.getStatusCode().equals(HttpStatusCode.valueOf(200))) {
                    break;
                }
            }
            if (status.equals("not_running") || status.equals("fail")) {
                throw new RuntimeException("getAnswersInLastSeconds: Status export is " + status);
            }
        }

        ResponseEntity<byte[]> responseCsv = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                entity,
                byte[].class
        );
        List<String> ids;

        if (response.getStatusCode().equals(HttpStatusCode.valueOf(200))) {
            String csvString = new String(responseCsv.getBody(), StandardCharsets.UTF_8);
            ids = getIdFromCsvResponse(csvString);
        } else {
            throw new RuntimeException("getAnswers: status code responseCsv: " + response.getStatusCode());
        }


        List<AnswerDTO> answerDTOS = new ArrayList<>();

        for (var i : ids) {
            String answerJson = getAnswerById(i);
            answerDTOS.add(jsonService.parseJsonToAnswer(answerJson));
        }

        for (var a : answerDTOS) {
            a.setCreatedAt(a.getCreatedAt().withZoneSameInstant(ZoneId.of("Europe/Moscow")));
        }

        return answerDTOS;
    }

    private List<String> getIdFromCsvResponse(String csvString) {
        List<String> ids = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new StringReader(csvString))) {
            csvReader.skip(1);
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                ids.add(line[0].trim());
            }
        } catch (Exception e) {
            throw new RuntimeException("getIdFromCsvResponse: " + e.getMessage());
        }
        log.debug("getIdFromCsvResponse(): ids: {}", ids);
        return ids;
    }

    public String getAnswerById(String id) {
        final String baseUrl = "https://api.forms.yandex.net/v1/answers";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + yandexToken.getAccessToken());
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("answer_id", id);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
