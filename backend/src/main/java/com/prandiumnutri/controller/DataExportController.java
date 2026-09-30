package com.prandiumnutri.controller;

import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.DataExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class DataExportController {

    private final DataExportService exportService;
    private final CurrentUser currentUser;

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        byte[] zip = exportService.exportForUser(currentUser.id());
        String filename = "prandium-nutri-export-" + LocalDate.now() + ".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(zip);
    }
}
