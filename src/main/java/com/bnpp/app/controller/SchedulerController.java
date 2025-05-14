package com.bnpp.app.controller;

import java.util.List;
import java.util.Map;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bnpp.app.service.SchedulerService;
import com.bnpp.app.shared.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class SchedulerController {

	@Autowired
	SchedulerService schedulerService;

	@PostMapping("/schedule-report")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN' ) or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> scheduleReport(@RequestParam("uc") String uc,
			@RequestParam("reportName") String reportName, @RequestParam("format") String format,
			@RequestParam("dataSourceName") String dataSourceName,
			@RequestParam("cronExpression") String cronExpression) {
		return schedulerService.scheduleReport(uc.trim(), reportName.trim(), format.trim(), dataSourceName.trim(),
				cronExpression.trim());
	}

	@DeleteMapping("/unschedule-report")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN' ) or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> unscheduleReport(@RequestParam("uc") String uc,
			@RequestParam("reportName") String reportName, @RequestParam("format") String format ) {
		return schedulerService.unscheduleReport(uc.trim(), reportName.trim(),format.trim());
	}

	@PostMapping("/pause-schedule")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN' ) or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> pauseSchedule(@RequestParam("uc") String uc,
			@RequestParam("reportName") String reportName, @RequestParam("format") String format) {
		return schedulerService.pauseSchedule(uc.trim(), reportName.trim(), format.trim());
	}

	@PostMapping("/resume-schedule")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN' ) or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Object>> resumeSchedule(@RequestParam("uc") String uc,
			@RequestParam("reportName") String reportName, @RequestParam("format") String format) {
		return schedulerService.resumeSchedule(uc.trim(), reportName.trim(), format.trim());
	}

	@GetMapping("/list-jobs")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN') or hasAuthority('TEAM_' + #uc.toUpperCase() + '_VIEWER') or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listJobs(@RequestParam("uc") String uc) {
		return schedulerService.listJobs(uc.trim());
	}

	@GetMapping("/get-jobs-info")
	//@PreAuthorize("hasAuthority('TEAM_' + #uc.toUpperCase() + '_ADMIN') or hasAuthority('TEAM_' + #uc.toUpperCase() + '_VIEWER') or hasRole('ADMIN')")
	public ResponseEntity<?> getJobsInfo(@RequestParam String uc) {
		return schedulerService.getJobsInfo(uc.trim());

	}
}
