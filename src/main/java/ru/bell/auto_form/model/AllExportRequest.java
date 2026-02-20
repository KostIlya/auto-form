package ru.bell.auto_form.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AllExportRequest {
    private List<String> columns;
    private String format;
    private String upload;
    private Boolean upload_files;
}