package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bell.auto_form.model.dto.AnswerDTO;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.*;

@Service
@Slf4j
public class XlsxService {
    public void appendAnswers(List<AnswerDTO> answers, String filePath) {
        Workbook workbook = null;
        try (FileInputStream file = new FileInputStream(filePath)) {
            workbook = WorkbookFactory.create(file);
            Sheet sheet = workbook.getSheetAt(0);

            for (AnswerDTO answerDTO : answers) {
                appendRowAnswer(answerDTO, sheet);
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        } catch (Exception e) {
            log.error("appendAnswer(): {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error("Error closing workbook: {}", e.getMessage(), e);
                }
            }
        }
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
}
