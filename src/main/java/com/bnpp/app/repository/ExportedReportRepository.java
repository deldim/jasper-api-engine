package com.bnpp.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bnpp.app.dto.ReportJobInfoDTO;
import com.bnpp.app.model.ExportedReport;

@Repository
public interface ExportedReportRepository extends JpaRepository<ExportedReport, Long> {

    Optional<ExportedReport> findByUcAndReportNameAndFormat(String uc, String reportName, String format);
    List<ExportedReport> findByUc(String uc);
    void deleteByUcAndReportNameAndFormat(String uc, String reportName, String format);
    boolean existsByUcAndReportNameAndFormat(String uc, String reportName, String format);
    @Query(value = """
        SELECT 
            er.report_name AS reportName,
            er.format AS format,
            er.size AS size,
            er.last_updated AS lastUpdated,
            FROM_UNIXTIME(qt.NEXT_FIRE_TIME / 1000.0) AS nextUpdate,
            qt.TRIGGER_STATE AS jobStatus,
            qc.CRON_EXPRESSION AS frequency,
            qt.PRIORITY AS priority,
            er.scheduled
        FROM exported_reports er
        LEFT JOIN qrtz_triggers qt
            ON CONCAT(er.report_name, '.', er.format) = qt.JOB_NAME
        LEFT JOIN qrtz_cron_triggers qc
            ON qt.TRIGGER_NAME = qc.TRIGGER_NAME
        WHERE er.uc = :uc
    """, nativeQuery = true)
    List<Object[]> findRawJobInfoByUc(@Param("uc") String uc);

}
