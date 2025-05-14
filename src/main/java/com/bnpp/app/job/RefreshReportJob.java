package com.bnpp.app.job;

import java.sql.SQLException;

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
			jasperReportService.generateReport(data.getString("uc"), data.getString("reportName"), data.getString("format"), data.getString("dataSourceName"));
		} catch (JRException | SQLException e) {
			e.printStackTrace();
		}
	}

}
