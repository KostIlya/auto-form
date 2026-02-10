package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bell.auto_form.model.dto.AnswerDTO;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.*;

@Service
@Slf4j
public class XlsxService {
    public Set<String> getAllExistsAnswersIds(String filePath) {
        try (FileInputStream file = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            return getAllExistsAnswersIds(sheet);
        } catch (Exception e) {
            log.error("appendAnswer(): {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Integer appendAnswers(List<AnswerDTO> answers, String filePath) {
        Integer countAnswersRecordings = 0;
        try (FileInputStream file = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);

            Set<String> existAnswersIds = getAllExistsAnswersIds(sheet);
            for (AnswerDTO answerDTO : answers) {
                if (!isExistAnswer(answerDTO.getId(), existAnswersIds)) {
                    countAnswersRecordings++;
                    existAnswersIds.add(answerDTO.getId());
                    appendRowAnswer(answerDTO, sheet);
                }
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        } catch (Exception e) {
            log.error("appendAnswer(): {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return countAnswersRecordings;
    }

    private Set<String> getAllExistsAnswersIds(Sheet sheet) {
        try {
            Set<String> existAnswersIds = new HashSet<>();

            int rowNum = sheet.getLastRowNum();
            for (int i = 1; i <= rowNum; i++) {
                Cell cell = sheet.getRow(i).getCell(0);
                if (cell != null) {
                    existAnswersIds.add(cell.getRichStringCellValue().getString());
                } else {
                    log.warn("Cell in row({}) is null.", i + 1);
                }
            }

            log.debug("getAllExistsAnswersIds(): numAnswers {}; existAnswersIds: {}", existAnswersIds.size(), existAnswersIds);
            return existAnswersIds;
        } catch (Exception e) {
            log.error("getAllExistsAnswersIds(): {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean isExistAnswer(String answerId, Set<String> existAnswersIds) {
        return existAnswersIds.contains(answerId);
    }

    private void appendRowAnswer(AnswerDTO answerDTO, Sheet sheet) {
        int lastRow = sheet.getLastRowNum();

        Row newRow = sheet.createRow(lastRow + 1);
        newRow.createCell(0).setCellValue(answerDTO.getId());
        newRow.createCell(1).setCellValue(answerDTO.getCreatedAt().toString());
        newRow.createCell(2).setCellValue(answerDTO.getFio());
        newRow.createCell(3).setCellValue(answerDTO.getAge());
        newRow.createCell(4).setCellValue(answerDTO.getLocation());
        newRow.createCell(5).setCellValue(answerDTO.getTelegram());
        newRow.createCell(6).setCellValue(answerDTO.getEmail());
        newRow.createCell(7).setCellValue(answerDTO.getHr());
        newRow.createCell(8).setCellValue(answerDTO.getResume());
        newRow.createCell(9).setCellValue(answerDTO.getQuestionnaire());
    }

    public String getId(Sheet sheet, int rowNumber) {
        Row row = sheet.getRow(rowNumber);

        return row.getCell(0).getStringCellValue();
    }

    public AnswerDTO getAnswerDTO(Sheet sheet, int rowNumber) {
        Row row = sheet.getRow(rowNumber);

        return AnswerDTO.builder()
                .id(getStringCellValue(row, 0))
                .createdAt(ZonedDateTime.parse(getStringCellValue(row, 1)))
                .fio(getStringCellValue(row, 2))
                .age((int) row.getCell(3).getNumericCellValue())
                .location(getStringCellValue(row, 4))
                .telegram(getStringCellValue(row, 5))
                .email(getStringCellValue(row, 6))
                .hr(getStringCellValue(row, 7))
                .resume(getStringCellValue(row, 8))
                .questionnaire(getStringCellValue(row, 9))
                .build();
    }

    private String getStringCellValue(Row row, int cellNum) {
        return row.getCell(cellNum) != null ? row.getCell(cellNum).getStringCellValue() : "";
    }
}
