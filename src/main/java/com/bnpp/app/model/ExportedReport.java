package com.bnpp.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exported_reports")
public class ExportedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uc", nullable = false)
    private String uc;

    @Column(name = "report_name", nullable = false)
    private String reportName;

    @Column(name = "format", nullable = false)
    private String format;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "scheduled", nullable = false)
    private boolean scheduled;    

    // ✅ Constructeurs
    public ExportedReport() {}

    public ExportedReport(String uc, String reportName, String format, Long size, LocalDateTime lastUpdated, boolean scheduled) {
        this.uc = uc;
    	this.reportName = reportName;
        this.format = format;
        this.size = size;
        this.lastUpdated = lastUpdated;
        this.scheduled = scheduled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUc() {
        return uc;
    }

    public void setUc(String uc) {
        this.uc = uc;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean getScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }


    
}

