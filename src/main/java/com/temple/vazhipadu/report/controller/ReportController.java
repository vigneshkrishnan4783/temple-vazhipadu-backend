package com.temple.vazhipadu.report.controller;

import com.temple.vazhipadu.report.dto.DailyCollectionReportResponse;
import com.temple.vazhipadu.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily-collection")
    public ResponseEntity<DailyCollectionReportResponse> getDailyCollectionReport(
            @RequestParam Long templeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyCollectionReportResponse report = reportService.generateDailyCollectionReport(templeId, date);
        return ResponseEntity.ok(report);
    }
}
