package com.bnpp.app.service;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bnpp.app.dao.DynamicDataSourceManager;
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
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignQuery;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;


@Service
public class JasperTemplateService {
	@Autowired
	DataSourceRepository dataSourceRepository;
	@Autowired
	JasperTemplateRepository jasperTemplateRepository;
	@Autowired
	ExportedReportRepository exportedReportRepository;
	@Value("${reports.repository.path}")
	private String repositoryPath;

	public ResponseEntity<ApiResponse<Object>> submitTemplate(MultipartFile file, String uc) throws IOException {
		String templateName = file.getOriginalFilename();
		// Validate file name
		if (templateName == null || templateName.isBlank() || !templateName.matches("^[\\w\\-.]+$")) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Invalid file name.", null));
		}
		// Optional: Enforce .jrxml extension
		if (!templateName.endsWith(".jrxml")) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Only .jrxml files are supported.", null));
		}
		String uploadPathString = repositoryPath + "/" + uc + "/templates/" + templateName;
		Path uploadPath = Paths.get(uploadPathString);
	
		// Check if file is empty
		if (file.isEmpty()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(false, "Uploaded template is empty.", null));
		}
		// Save the file to disk
		try {
			Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(false, "Failed to save file. Make sure the UC directory exists and is writable.", null));
		}
	
		// Update or create template record
		if (jasperTemplateRepository.existsByUcAndSourceFile(uc, templateName)) {
			JasperTemplate template = jasperTemplateRepository.findByUcAndSourceFile(uc, templateName);
			template.setCreationDate(LocalDateTime.now());
			template.setValidated(false);
			template.setValidationDate(null);
			jasperTemplateRepository.save(template);
			return ResponseEntity.ok(new ApiResponse<>(true, "Template "+templateName+" updated successfully.", null));
		} else {
			JasperTemplate template = new JasperTemplate();
			template.setUc(uc);
			template.setSourceFile(templateName);
			template.setUploader("b58643");
			template.setValidated(false);
			template.setCreationDate(LocalDateTime.now());
			template.setValidationDate(null);
			jasperTemplateRepository.save(template);
			return ResponseEntity.ok(new ApiResponse<>(true, "Template "+templateName+" created successfully.", null));
		}
	}
	
	public ResponseEntity<ApiResponse<Object>> validateTemplate(String uc, String templateName, String dataSourceName) throws JRException, SQLException {
		Optional<DataSource> optionalDataSource = dataSourceRepository.findByUcAndName(uc, dataSourceName);
		if (!optionalDataSource.isPresent()) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Referenced data source is missing.", null));
		}
		String connectionString = optionalDataSource.get().getHostName() + ":" +
								  optionalDataSource.get().getPort() + "/" +
								  optionalDataSource.get().getDbName() +
								  "@root@jaimelestomates?1";
		HikariDataSource datasource = DynamicDataSourceManager.buildDataSource(connectionString);
		String templatesFilePath = repositoryPath + uc + "/templates/";
		String compiledFilePath = repositoryPath + uc + "/compiled/";
		String jrxmlFile = templateName + ".jrxml";
		String jasperFile = templateName + ".jasper";
		JasperReport jasperReport;
		try {
			JasperDesign design = JRXmlLoader.load(templatesFilePath + jrxmlFile);
			if (design.getQuery() == null || design.getQuery().getText() == null) {
				return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Template does not contain a query.", null));
			}
			String originalQuery = design.getQuery().getText();
			JRDesignQuery newQuery = new JRDesignQuery();
			newQuery.setText(originalQuery + " LIMIT 1");
			jasperReport = JasperCompileManager.compileReport(design);
			JRDesignQuery oldQuery = new JRDesignQuery();
			oldQuery.setText(originalQuery);			
			design.setQuery(oldQuery);
			JasperCompileManager.compileReportToFile(design, compiledFilePath + jasperFile);
		} catch (JRException e) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Template compilation failed.", e.getMessage()));
		}
		try (Connection connection = datasource.getConnection()) {
			Map<String, Object> parameters = new HashMap<>();
			JasperFillManager.fillReport(jasperReport, parameters, connection);
		} catch (JRException e) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Data injection failed.", e.getMessage()));
		} finally {
			datasource.close();
		}
		JasperTemplate template = jasperTemplateRepository.findByUcAndSourceFile(uc, jrxmlFile);
		template.setValidated(true);
		template.setValidationDate(LocalDateTime.now());
		jasperTemplateRepository.save(template);
		return ResponseEntity.ok(new ApiResponse<>(true, "Template validated successfully.", null));
	}
	
	public byte[] getTemplate(String uc, String templateName) throws ReportNotFoundException {
		String filePathString = repositoryPath + "/" + uc + "/templates/" + templateName + ".jrxml";
		Path path = Paths.get(filePathString);

		if (!Files.exists(path)) {
			throw new ReportNotFoundException("Requested template file doesn't exist.");
		}

		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new ReportNotFoundException("Failed to read the template file.", e);
		}
	}
		
	public ResponseEntity<ApiResponse<Object>> listTemplates(String uc) {
		if (uc == null || uc.trim().isEmpty()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "UC must be provided.", null));
		}
		List<JasperTemplate> templates = jasperTemplateRepository.findByUc(uc);
		String message = templates.isEmpty() ? "No templates found." : "Request executed successfully.";
		return ResponseEntity.ok(new ApiResponse<>(true, message, templates));
	}
	
	@Transactional
	public ResponseEntity<ApiResponse<Object>> deleteTemplate(String uc, String templateName) {
		Path dir = Paths.get(repositoryPath + uc + "/templates/");
		if (!Files.exists(dir) || !Files.isDirectory(dir)) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Invalid directory path: UC directory is missing.", null));
		}
		List<Path> matchingFiles;
		try (Stream<Path> stream = Files.list(dir)) {
			matchingFiles = stream
				.filter(path -> path.getFileName().toString().equals(templateName))
				.toList();
		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Failed to list template directory.", null));
		}
		if (matchingFiles.isEmpty()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "No matching file found.", null));
		}
		List<String> failedDeletes = new ArrayList<>();
		for (Path path : matchingFiles) {
			try {
				Files.delete(path);
			} catch (IOException e) {
				failedDeletes.add(path.getFileName().toString());
			}
		}
		if (!failedDeletes.isEmpty()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(false, "Failed to delete template: " + failedDeletes, null));
		}
		if (!jasperTemplateRepository.existsByUcAndSourceFile(uc, templateName)) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "No matching database entry found.", null));
		}
		jasperTemplateRepository.deleteByUcAndSourceFile(uc, templateName);
		return ResponseEntity.ok(new ApiResponse<>(true, "Template "+templateName+" successfully removed.", null));
	}

}
