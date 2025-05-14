package com.bnpp.app.controller;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bnpp.app.service.JasperReportService;
import com.bnpp.app.shared.ApiResponse;

import net.sf.jasperreports.engine.JRException;

@RestController
//@SecurityRequirement(name = "bearerAuth")
public class ReportController {

	@Autowired
	JasperReportService jasperReportService;
	@Value("${reports.repository.path}")
	private String repositoryPath;

	@PostMapping("/generate-report")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN' ) or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> generateReport(@RequestParam("uc") String uc,
			@RequestParam("templateName") String templateName, @RequestParam("format") String format,
			@RequestParam("dataSourceName") String dataSourceName) throws JRException, SQLException {
		return jasperReportService.generateReport(uc.trim(), templateName.trim(), format.trim(), dataSourceName.trim());
	}

	@GetMapping("/get-report")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN') or hasAuthority('TEAM_' + #uc.toUpperCase() + '_VIEWER') or hasRole('ADMIN')")
	public ResponseEntity<Resource> getReport(@RequestParam String uc, @RequestParam String report_name,
			@RequestParam String format) throws JRException, IOException {
		byte[] reportContent = jasperReportService.getReport(uc.trim(), report_name.trim(), format.trim());
		ByteArrayResource resource = new ByteArrayResource(reportContent);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
				.contentLength(resource.contentLength()).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
						.attachment().filename(report_name.trim() + "." + format.trim()).build().toString())
				.body(resource);
	}

	@GetMapping("/list-reports")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN') or hasAuthority('TEAM_' + #uc.toUpperCase() + '_VIEWER') or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> listReports(@RequestParam String uc) {
		return jasperReportService.listReports(uc.trim());
	}

	@DeleteMapping("/delete-report")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN' ) or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> deleteReport(@RequestParam("uc") String uc,
			@RequestParam("reportName") String reportName,@RequestParam("format") String format) {
		return jasperReportService.deleteReport(uc.trim(), reportName.trim(),format.trim());
	}

}
