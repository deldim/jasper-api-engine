package com.bnpp.app.model;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "jasper_templates")
public class JasperTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uc", nullable = false)
    private String uc;

    @Column(name = "source_file", nullable = false)
    private String sourceFile;

    @Column(name = "uploader", nullable = false)
    private String uploader;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "validation_date", nullable = true)
    private LocalDateTime validationDate;

    @Column(name = "validated", nullable = false)
    private boolean validated;

    // ✅ Constructors
    public JasperTemplate() {}

    public JasperTemplate(String uc, String sourceFile, LocalDateTime creationDate, String uploader, boolean validated) {
    	this.uc=uc;
        this.sourceFile = sourceFile;
        this.creationDate = creationDate;
        this.uploader = uploader;
        this.validated = validated;
    }

    // ✅ Getters and Setters

    public Long getId() {
        return id;
    }

    public String getUc() {
        return uc;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setUc(String uc) {
        this.uc = uc;
    }
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public LocalDateTime getValidationDate() {
        return validationDate;
    }

    public void setValidationDate(LocalDateTime validationDate) {
        this.validationDate = validationDate;
    }

    
}
