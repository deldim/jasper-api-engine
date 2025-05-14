package com.bnpp.app.shared;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JasperReportsContext;

public class JasperReportsSecurityConfig {
    public static void configureClassFilter() {
        JasperReportsContext jasperReportsContext = DefaultJasperReportsContext.getInstance();

        // Enable the class filter
        jasperReportsContext.setProperty("net.sf.jasperreports.report.class.filter.enabled", "true");

        // Configure allowed classes (whitelist) - add the classes your reports need
        jasperReportsContext.setProperty("net.sf.jasperreports.report.class.filter.classes",
                "java.lang.String," +
                "java.lang.Integer," +
                "java.lang.Double," +
                "java.lang.Float," +
                "java.lang.Boolean," +
                "java.util.Date," +
                "java.math.BigDecimal," +
                "java.util.ArrayList," +
                "java.util.HashMap," +
                "java.util.HashSet," +
                "net.sf.jasperreports.engine.JRField," +
                "net.sf.jasperreports.engine.JRParameter," +
                "net.sf.jasperreports.engine.JRVariable" // Add other necessary JasperReports classes
        );

        // Configure allowed packages (whitelist) - add the packages your reports need
        jasperReportsContext.setProperty("net.sf.jasperreports.report.class.filter.packages",
                "java.time," + // If you are using Java 8+ Date and Time API
                "org.apache.commons.lang3," + // Example: Apache Commons Lang
                "org.apache.commons.collections4" // Example: Apache Commons Collections
        );

        // Configure blocked classes (blacklist) - add any classes you want to explicitly prevent
        jasperReportsContext.setProperty("net.sf.jasperreports.report.class.filter.blocked.classes",
                "java.lang.Runtime," +
                "java.lang.ProcessBuilder," +
                "java.io.File" // Example: Prevent file system access
        );

        // Configure blocked packages (blacklist) - add any packages you want to explicitly prevent
        jasperReportsContext.setProperty("net.sf.jasperreports.report.class.filter.blocked.packages",
                "java.net," +
                "java.rmi"
        );
    }
}
