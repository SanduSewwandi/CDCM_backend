package com.example.demo.dto;

public class ReportUploadRequest {
    private String reportUrl;
    private String reportText;

    public ReportUploadRequest() {}

    public String getReportUrl() { return reportUrl; }
    public void setReportUrl(String reportUrl) { this.reportUrl = reportUrl; }

    public String getReportText() { return reportText; }
    public void setReportText(String reportText) { this.reportText = reportText; }
}