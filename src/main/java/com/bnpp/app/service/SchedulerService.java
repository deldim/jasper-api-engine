package com.bnpp.app.service;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.CronExpression;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.bnpp.app.dao.DynamicDataSourceManager;
import com.bnpp.app.dto.ReportJobInfoDTO;
import com.bnpp.app.job.RefreshReportJob;
import com.bnpp.app.model.DataSource;
import com.bnpp.app.model.ExportedReport;
import com.bnpp.app.repository.DataSourceRepository;
import com.bnpp.app.repository.ExportedReportRepository;
import com.bnpp.app.shared.ApiResponse;

import java.time.Duration;

@Service
public class SchedulerService {
	@Autowired
	DataSourceRepository dataSourceRepository;
	@Autowired
	Scheduler scheduler;
	@Autowired
	ExportedReportRepository exportedReportRepository;
	@Value("${reports.repository.path}")
	private String repositoryPath;
	@Value("${scheduler.minimum.interval.minutes}")
	private int minimumIntervalMinutes;
	public ResponseEntity<ApiResponse<Object>> scheduleReport(
		String uc,
		String reportName,
		String format,
		String dataSourceName,
		String cronExpression,
		Map<String, Object> parameters) {
		// 1. Validate UC directory
		File dir = new File(repositoryPath + "/" + uc + "/");
		if (!dir.exists() || !dir.isDirectory()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "UC directory is missing", null));
		}
		// 2. Check compiled report exists
		Path compiledPath = Paths.get(repositoryPath + uc + "/compiled/" + reportName + ".jasper");
		if (!Files.exists(compiledPath)) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Compiled report is missing", null));
		}
		// 3. Validate format
		Set<String> supportedFormats = Set.of("csv", "xlsx", "html", "xml", "doc", "pdf");
		if (!supportedFormats.contains(format)) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Unknown report format", null));
		}
		// 4. Validate and test data source
		Optional<DataSource> optionalDataSource = dataSourceRepository.findByUcAndName(uc, dataSourceName);
		if (optionalDataSource.isEmpty()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Data source not found", null));
		}
		String connectionString = optionalDataSource.get().getHostName()
				+ ":" + optionalDataSource.get().getPort()
				+ "/" + optionalDataSource.get().getDbName()
				+ "@root@jaimelestomates?1";
		if (!DynamicDataSourceManager.testConnection(connectionString)) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Database connection failed", null));
		}
		// 5. Validate cron expression using centralized validator
		ResponseEntity<ApiResponse<Object>> validationResult = validateCronExpression(cronExpression,minimumIntervalMinutes);
		if (!validationResult.getBody().isSuccess()) {
			return validationResult;
		}
		try {
			String jobKeyName = reportName + "." + format;
			JobKey jobKey = JobKey.jobKey(jobKeyName, uc);
			TriggerKey triggerKey = TriggerKey.triggerKey(jobKeyName, uc);
			if (scheduler.checkExists(jobKey)) {
				return ResponseEntity.badRequest()
						.body(new ApiResponse<>(false, "Job is already scheduled", null));
			}
			// Random delay between 10 and 60 minutes (step of 10)
			int delayMinutes = ThreadLocalRandom.current().nextInt(1, 7) * 10;
			Date startAt = Date.from(Instant.now().plus(Duration.ofMinutes(delayMinutes)));
			JobDataMap jobDataMap = new JobDataMap();

			// Add individual scalar parameters
			jobDataMap.put("uc", uc);
			jobDataMap.put("reportName", reportName);
			jobDataMap.put("format", format);
			jobDataMap.put("dataSourceName", dataSourceName);

			// Add a Map<String, Object> (parameters)
			if (parameters != null) {
				jobDataMap.put("parameters", parameters);  // Ensure parametersMap is Serializable
			}
			JobDetail job = JobBuilder.newJob(RefreshReportJob.class)
					.withIdentity(jobKey)
					.setJobData(jobDataMap)
					.storeDurably()
					.build();
			CronTrigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerKey)
					.startAt(startAt)
					.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
							.withMisfireHandlingInstructionDoNothing())
					.build();
			scheduler.scheduleJob(job, trigger);
			Optional<ExportedReport> optionalExportedReport = exportedReportRepository
					.findByUcAndReportNameAndFormat(uc, reportName, format);
			if (optionalExportedReport.isEmpty()) {
				return ResponseEntity.badRequest()
						.body(new ApiResponse<>(false, "Exported report not found", null));
			}
			ExportedReport exportedReport = optionalExportedReport.get();
			exportedReport.setScheduled(true);
			exportedReportRepository.save(exportedReport);
			return ResponseEntity.ok(new ApiResponse<>(true,
					"Report " + jobKeyName + " scheduled successfully.", null));
		} catch (SchedulerException e) {
			e.printStackTrace();
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Scheduling failed: " + e.getMessage(), null));
		}
	}    

	public ResponseEntity<ApiResponse<Object>> unscheduleReport(String uc, String reportName, String format) {
		String keyName = reportName + "." + format;
		TriggerKey triggerKey = TriggerKey.triggerKey(keyName, uc);
		JobKey jobKey = JobKey.jobKey(keyName, uc);
	
		try {
			boolean triggerRemoved = scheduler.unscheduleJob(triggerKey);
			if (!triggerRemoved) {
				return ResponseEntity.badRequest().body(
					new ApiResponse<>(false, "Trigger not found for job " + keyName + ".", null));
			}
	
			boolean jobDeleted = scheduler.deleteJob(jobKey);
			if (!jobDeleted) {
				return ResponseEntity.badRequest().body(
					new ApiResponse<>(false, "Job not found for deletion: " + keyName, null));
			}
	
			// Update DB flag
			exportedReportRepository.findByUcAndReportNameAndFormat(uc, reportName, format)
				.ifPresent(exportedReport -> {
					exportedReport.setScheduled(false);
					exportedReportRepository.save(exportedReport);
				});
	
			return ResponseEntity.ok(new ApiResponse<>(true,
				"Job and trigger successfully unscheduled for: " + keyName, null));
	
		} catch (SchedulerException e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(
				new ApiResponse<>(false, "Failed to unschedule job due to scheduler error.", null));
		}
	}
	
	public ResponseEntity<ApiResponse<Object>> pauseSchedule(String uc, String reportName, String format) {
		String triggerName = reportName + "." + format;
		TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, uc);
	
		try {
			if (!scheduler.checkExists(triggerKey)) {
				return ResponseEntity.badRequest().body(
					new ApiResponse<>(false, "Trigger not found for " + triggerName, null));
			}
	
			scheduler.pauseTrigger(triggerKey);
	
			return ResponseEntity.ok(new ApiResponse<>(
				true,
				"Scheduling paused successfully for report: " + triggerName,
				null
			));
	
		} catch (SchedulerException e) {
			e.printStackTrace(); // Optional: use logger instead
			return ResponseEntity.badRequest().body(
				new ApiResponse<>(false, "Failed to pause scheduling for report: " + triggerName, e.getMessage()));
		}
	}	

	public ResponseEntity<ApiResponse<Object>> resumeSchedule(String uc, String reportName, String format) {
		String triggerName = reportName + "." + format;
		TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, uc);
	
		try {
			if (!scheduler.checkExists(triggerKey)) {
				return ResponseEntity.badRequest().body(
					new ApiResponse<>(false, "Trigger not found for " + triggerName, null));
			}
	
			scheduler.resumeTrigger(triggerKey);
	
			return ResponseEntity.ok(
				new ApiResponse<>(true, "Report " + triggerName + " scheduling resumed successfully.", null));
	
		} catch (SchedulerException e) {
			e.printStackTrace(); // Consider replacing with a logger in real apps
			return ResponseEntity.badRequest().body(
				new ApiResponse<>(false, "Failed to resume scheduling for report: " + triggerName, e.getMessage()));
		}
	}	
	
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listJobs(String uc) {
		if (uc == null || uc.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(false, "UC must be provided.", null));
		}
		List<Map<String, Object>> jobList = new ArrayList<>();
		try {
			for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(uc))) {
				JobDetail jobDetail = scheduler.getJobDetail(jobKey);
				List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
				for (Trigger trigger : triggers) {
					Map<String, Object> jobInfo = new HashMap<>();
					jobInfo.put("jobName", jobKey.getName());
					jobInfo.put("jobGroup", jobKey.getGroup());
					jobInfo.put("jobClass", jobDetail.getJobClass().getName());
					jobInfo.put("description", jobDetail.getDescription());
					jobInfo.put("isDurable", jobDetail.isDurable());
					jobInfo.put("requestsRecovery", jobDetail.requestsRecovery());
					jobInfo.put("jobData", jobDetail.getJobDataMap());
					jobInfo.put("nextFireTime", trigger.getNextFireTime());
					jobInfo.put("previousFireTime", trigger.getPreviousFireTime());
					jobInfo.put("triggerState", scheduler.getTriggerState(trigger.getKey()).name());
					if (trigger instanceof CronTrigger cronTrigger) {
						jobInfo.put("cronExpression", cronTrigger.getCronExpression());
					}
					jobList.add(jobInfo);
				}
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(
				new ApiResponse<>(false, "Failed to fetch job list: " + e.getMessage(), null)
			);
		}
		return ResponseEntity.ok(new ApiResponse<>(true, "Job list retrieved successfully.", jobList));
	}
	
	public ResponseEntity<ApiResponse<Object>> getJobsInfo(String uc) {
		List<Object[]> rawData = exportedReportRepository.findRawJobInfoByUc(uc);
	
		if (rawData == null || rawData.isEmpty()) {
			return ResponseEntity.ok(new ApiResponse<>(true, "No job data available.", List.of()));
		}
		List<ReportJobInfoDTO> result = rawData.stream().map(row -> {
			Boolean isScheduled = null;
			Object scheduledObj = row[8];
			if (scheduledObj instanceof Boolean) {
				isScheduled = (Boolean) scheduledObj;
			} else if (scheduledObj instanceof Number) {
				isScheduled = ((Number) scheduledObj).intValue() != 0;
			}
			return new ReportJobInfoDTO(
				(String) row[0],                       // reportName
				(String) row[1],                       // format
				(Long) row[2],                         // size
				(Timestamp) row[3],                    // lastUpdated
				(Timestamp) row[4],                    // nextUpdate
				(String) row[5],                       // jobStatus
				(String) row[6],                       // frequency
				row[7] != null ? ((Number) row[7]).intValue() : null, // priority
				isScheduled
			);
		}).toList();
		return ResponseEntity.ok(new ApiResponse<>(true, "Request executed successfully.", result));
	}

	private ResponseEntity<ApiResponse<Object>> validateCronExpression(String cronExpression, int minimumIntervalMinutes) {
		String[] cronParts = cronExpression.trim().split(" ");
		if (cronParts.length < 6) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Malformed cron expression", null));
		}
	
		if (!CronExpression.isValidExpression(cronExpression)) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Invalid cron expression", null));
		}
	
		try {
			CronExpression cron = new CronExpression(cronExpression);
			Date first = cron.getNextValidTimeAfter(new Date());
			Date second = cron.getNextValidTimeAfter(first);
			long interval = Duration.between(first.toInstant(), second.toInstant()).toMinutes();
			if (interval < minimumIntervalMinutes) {
				return ResponseEntity.badRequest()
						.body(new ApiResponse<>(false, "Minimum allowed frequency is 10 minutes.", null));
			}
		} catch (ParseException e) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(false, "Malformed cron expression", null));
		}
		return ResponseEntity.ok(new ApiResponse<>(true, "Cron expression is valid.", null));
	}
}

