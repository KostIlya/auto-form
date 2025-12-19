package ru.bell.auto_form.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    private List<String> columns;
    private String started_at;
    private String finished_at;
    private String format;
    private String upload;
    private Boolean upload_files;
}