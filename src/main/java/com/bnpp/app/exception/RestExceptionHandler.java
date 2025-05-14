package com.bnpp.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import net.sf.jasperreports.engine.JRException;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler{

	@ExceptionHandler(value= {ReportNotFoundException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	protected ErrorResponse handleReportNotFoundException(ReportNotFoundException ex, WebRequest req){
		ErrorResponse err = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, new java.util.Date());
		return err;
	}

	@ExceptionHandler(value= {JRException.class})
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	protected ErrorResponse handleReportNotFoundException(JRException ex, WebRequest req){
		ErrorResponse err = new ErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, new java.util.Date());
		return err;
	}
}
