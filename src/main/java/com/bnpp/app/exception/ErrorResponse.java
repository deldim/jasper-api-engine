package com.bnpp.app.exception;

import java.util.Date;

import org.springframework.http.HttpStatus;

public class ErrorResponse {
	String message;
	HttpStatus httpStatus;
	Date time;
	public ErrorResponse(String message, HttpStatus httpStatus, Date time) {
		super();
		this.message = message;
		this.httpStatus = httpStatus;
		this.time = time;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(HttpStatus httpStatus) {
		this.httpStatus = httpStatus;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

}
