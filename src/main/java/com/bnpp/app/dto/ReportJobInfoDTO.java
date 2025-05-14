package com.bnpp.app.dto;

public class ReportJobInfoDTO {
    private String reportName;
    private String format;
    private Long size;
    private java.sql.Timestamp lastUpdated;
    private java.sql.Timestamp nextUpdate;
    private String jobStatus;
    private String frequency;
    private Integer priority;
    private Boolean scheduled;

    public ReportJobInfoDTO(String reportName, String format, Long size,
                            java.sql.Timestamp lastUpdated, java.sql.Timestamp nextUpdate,
                            String jobStatus, String frequency, Integer priority, Boolean scheduled) {
        this.reportName = reportName;
        this.format = format;
        this.size = size;
        this.lastUpdated = lastUpdated;
        this.nextUpdate = nextUpdate;
        this.jobStatus = jobStatus;
        this.frequency = frequency;
        this.priority = priority;
        this.scheduled = scheduled;
    }

    // ✅ Add getters only (no setters needed if read-only)
    public String getReportName() { return reportName; }
    public String getFormat() { return format; }
    public Long getSize() { return size; }
    public java.sql.Timestamp getLastUpdated() { return lastUpdated; }
    public java.sql.Timestamp getNextUpdate() { return nextUpdate; }
    public String getJobStatus() { return jobStatus; }
    public String getFrequency() { return frequency; }
    public Integer getPriority() { return priority; }
    public Boolean getScheduled() { return scheduled; }

}
