package com.bnpp.app.job;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import com.bnpp.app.service.JasperReportService;

import net.sf.jasperreports.engine.JRException;

public class RefreshReportJob implements Job {
	@Autowired
	JasperReportService jasperReportService;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap data = context.getMergedJobDataMap();
		try {
			Map<String, Object> parametersMap = (Map<String, Object>) data.get("parameters");
			jasperReportService.generateReport(
				data.getString("uc"),
				data.getString("reportName"),
				data.getString("format"),
				data.getString("dataSourceName"),
				parametersMap != null ? parametersMap : new HashMap<String, Object>()
			);
		} catch (JRException | SQLException e) {
			e.printStackTrace();
		}
	}

}
