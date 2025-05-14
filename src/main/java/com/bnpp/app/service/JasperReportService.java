package com.bnpp.app.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.sql.Timestamp;

import javax.print.attribute.standard.JobName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import com.bnpp.app.dao.DynamicDataSourceManager;
import com.bnpp.app.dto.ReportJobInfoDTO;
import com.bnpp.app.exception.ReportNotFoundException;
import com.bnpp.app.model.DataSource;
import com.bnpp.app.model.ExportedReport;
import com.bnpp.app.model.JasperTemplate;
import com.bnpp.app.repository.DataSourceRepository;
import com.bnpp.app.repository.ExportedReportRepository;
import com.bnpp.app.repository.JasperTemplateRepository;
import com.bnpp.app.shared.ApiResponse;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.transaction.Transactional;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignQuery;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXmlExporterOutput;

@Service
public class JasperReportService {
	@Autowired
	DataSourceRepository dataSourceRepository;
	@Autowired
	SchedulerService schedulerService;
	@Autowired
	ExportedReportRepository exportedReportRepository;
	@Value("${reports.repository.path}")
	private String repositoryPath;

	
	public ResponseEntity<ApiResponse<Object>> generateReport(String uc, String reportName, String format,
                                                          String dataSourceName) throws JRException, SQLException {
    // Step 1: Validate data source
    Optional<DataSource> optionalDataSource = dataSourceRepository.findByUcAndName(uc, dataSourceName);
    if (!optionalDataSource.isPresent()) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Referenced data source is missing.", null));
    }
    // Step 2: Build connection string and open connection
    String connectionString = optionalDataSource.get().getHostName() + ":" +
                              optionalDataSource.get().getPort() + "/" +
                              optionalDataSource.get().getDbName() +
                              "@root@jaimelestomates?1";
    HikariDataSource datasource = DynamicDataSourceManager.buildDataSource(connectionString);
    String compiledFile = repositoryPath + uc + "/compiled/" + reportName + ".jasper";
    String outputFile = repositoryPath + uc + "/reports/" + reportName + "." + format;
    JasperPrint jasperPrint;
    try (
        Connection connection = datasource.getConnection();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    ) {
        // Step 3: Fill report
        jasperPrint = JasperFillManager.fillReport(compiledFile, null, connection);
        if (jasperPrint == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to fill report "+reportName+".", null));
        }
        // Step 4: Export to specified format
        switch (format) {
            case "csv":
                JRCsvExporter csvExporter = new JRCsvExporter();
                csvExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                csvExporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
                csvExporter.exportReport();
                break;

            case "xlsx":
                JRXlsxExporter xlsxExporter = new JRXlsxExporter();
                xlsxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xlsxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                xlsxExporter.exportReport();
                break;

            case "html":
                HtmlExporter htmlExporter = new HtmlExporter();
                htmlExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                htmlExporter.setExporterOutput(new SimpleHtmlExporterOutput(outputStream));
                htmlExporter.exportReport();
                break;

            case "xml":
                JRXmlExporter xmlExporter = new JRXmlExporter();
                xmlExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xmlExporter.setExporterOutput(new SimpleXmlExporterOutput(outputStream));
                xmlExporter.exportReport();
                break;

            case "doc":
                JRRtfExporter docxExporter = new JRRtfExporter();
                docxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                docxExporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
                docxExporter.exportReport();
                break;

            case "pdf":
                JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
                break;

            default:
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Unknown report format: " + format, null));
        }
        // Step 5: Write to file system
        Files.write(Paths.get(outputFile), outputStream.toByteArray());
    } catch (JRException | IOException e) {
        e.printStackTrace();
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Report generation failed: " + e.getMessage(), null));
    } finally {
        datasource.close();
    }
    // Step 6: Track report size and update database
    long sizeInBytes;
    try {
        sizeInBytes = Files.size(Paths.get(outputFile));
    } catch (IOException e) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Failed to read exported report size.", null));
    }
    Optional<ExportedReport> optionalExportedReport = exportedReportRepository
            .findByUcAndReportNameAndFormat(uc, reportName, format);
    if (optionalExportedReport.isPresent()) {
        ExportedReport existing = optionalExportedReport.get();
        existing.setSize(sizeInBytes);
        existing.setLastUpdated(LocalDateTime.now());
        exportedReportRepository.save(existing);
    } else {
        ExportedReport newReport = new ExportedReport(
                uc, reportName, format, sizeInBytes, LocalDateTime.now(), false
        );
        exportedReportRepository.save(newReport);
    }
    return ResponseEntity.ok(new ApiResponse<>(true, "Report "+reportName + "." + format+" generated successfully.", null));
}

	public byte[] getReport(String uc, String reportName, String format) throws ReportNotFoundException {
		String filePathString = repositoryPath + "/" + uc + "/reports/" + reportName + "." + format;
		Path path = Paths.get(filePathString);
		if (!Files.exists(path)) {
			throw new ReportNotFoundException("Requested report doesn't exist.");
		}
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new ReportNotFoundException("Failed to read the report file.", e);
		}
	}
	
    @Transactional
    public ResponseEntity<ApiResponse<Object>> deleteReport(String uc, String reportName, String format) {
        String reportFullName = reportName + "." + format;
        Path dir = Paths.get(repositoryPath + uc + "/reports/");
        Path reportPath = dir.resolve(reportFullName);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Invalid directory path, UC directory is missing.", null));
        }
        if (!Files.exists(reportPath)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "No matching report found.", null));
        }
        try {
            Files.delete(reportPath);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to delete report file.", e.getMessage()));
        }
        Optional<ExportedReport> exportedReport = exportedReportRepository
                .findByUcAndReportNameAndFormat(uc, reportName, format);
        if (!exportedReport.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "No matching report record found.", null));
        }
        if (exportedReport.get().getScheduled()) {
            schedulerService.unscheduleReport(uc, reportName, format);
        }
        exportedReportRepository.deleteByUcAndReportNameAndFormat(uc, reportName, format);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report successfully "+reportName + "." + format+" deleted.", null));
    }
    
	public ResponseEntity<ApiResponse<Object>> listReports(String uc) {
		if (uc == null || uc.trim().isEmpty()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "UC must be provided.", null));
		}
		List<ExportedReport> reports = exportedReportRepository.findByUc(uc);
		String message = reports.isEmpty()
				? "No reports found for the given UC."
				: "Request executed successfully.";
		return ResponseEntity.ok(new ApiResponse<>(true, message, reports));
	}
}
