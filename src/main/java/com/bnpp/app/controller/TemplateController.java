package com.bnpp.app.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bnpp.app.service.JasperTemplateService;
import com.bnpp.app.shared.ApiResponse;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import net.sf.jasperreports.engine.JRException;

@RestController
//@SecurityRequirement(name = "bearerAuth")
public class TemplateController {

	@Autowired
	JasperTemplateService jasperReportService;
	@Value("${reports.repository.path}")
	private String repositoryPath;


	@PostMapping(path = "/submit-template/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN' ) or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> submitTemplate(@RequestPart("file") MultipartFile file,
			@RequestParam("uc") String uc) throws JRException, IOException {
		if (!isSupportedContentType(file.getContentType())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponse<>(false, "Unsupported file type.", null));
		}
		if (!isValidFileName(file.getOriginalFilename())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponse<>(false, "Invalid file extension.", null));
		}
		if (file.getSize() > 2_000_000) { // 2 MB size limit
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponse<>(false, "File size exceeds limit.", null));
		}
		return jasperReportService.submitTemplate(file, uc.trim().toLowerCase());
	}

	@PostMapping("/validate-template")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN' ) or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> validateTemplate(
			@RequestParam("uc") String uc,
			@RequestParam("templateName") String templateName,
			@RequestParam("dataSourceName") String dataSourceName,
			@RequestBody(required = false) LinkedHashMap<String, Object> parameters
	) throws JRException, SQLException {

		Map<String, Object> safeParameters = (parameters == null || parameters.isEmpty())
			? new HashMap<>()
			: parameters;

		return jasperReportService.validateTemplate(
			uc.trim(),
			templateName.trim(),
			dataSourceName.trim(),
			safeParameters
		);
	}


	@GetMapping("/get-template")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN') or hasAuthority('TEAM_' + #uc.toUpperCase() + '_VIEWER') or hasRole('ADMIN')")
	public ResponseEntity<Resource> getTemplate(@RequestParam String uc, @RequestParam String report_name) throws JRException, IOException {

		byte[] reportContent = jasperReportService.getTemplate(uc.trim(), report_name.trim());
		ByteArrayResource resource = new ByteArrayResource(reportContent);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
				.contentLength(resource.contentLength()).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
						.attachment().filename(report_name.trim() + ".jrxml").build().toString())
				.body(resource);
	}

	@GetMapping("/list-templates")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN') or hasAuthority('TEAM_' + #uc.toUpperCase() + '_VIEWER') or hasRole('ADMIN')")
	public ResponseEntity<?> listTemplates(@RequestParam String uc) {
		return jasperReportService.listTemplates(uc.trim());

	}

	@DeleteMapping("/delete-template")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN' ) or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> deleteTemplate(@RequestParam("uc") String uc,
			@RequestParam("fileName") String fileName) {
		return jasperReportService.deleteTemplate(uc.trim(), fileName.trim());
	}
	
	private boolean isSupportedContentType(String contentType) {
		return contentType.equals("application/octet-stream");
	}

	private boolean isValidFileName(String fileName) {
		return fileName.trim().endsWith(".jrxml");
	}

}
