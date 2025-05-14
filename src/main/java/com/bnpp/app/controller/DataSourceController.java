package com.bnpp.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bnpp.app.service.DataSourceService;
import com.bnpp.app.shared.ApiResponse;

@RestController
//@SecurityRequirement(name = "bearerAuth")
public class DataSourceController {
	@Autowired
	DataSourceService dataSourceService;

	@GetMapping("/list-datasources")
	public ResponseEntity<ApiResponse<Object>>  getDataSourcesNames(@RequestParam("uc") String uc){
		return dataSourceService.getDataSourcesNames(uc);
	}

	@GetMapping("/get-datasources-details")
	public ResponseEntity<ApiResponse<Object>>  getDataSourcesDetails(@RequestParam("uc") String uc){
		return dataSourceService.getDataSourcesDetails(uc);
	}

	@PostMapping("/add-datasource")
	public  ResponseEntity<ApiResponse<Object>> addDataSource(@RequestParam("uc") String uc, @RequestParam("dataSourceName") String name, @RequestParam("hostName") String hostName, @RequestParam("port") String port, @RequestParam("dbName") String dbName){
		return dataSourceService.addDataSource(name, hostName, port, dbName, uc);
	}

	@DeleteMapping("/delete-datasource")
	public  ResponseEntity<ApiResponse<Object>> deleteDataSource(@RequestParam("uc") String uc, @RequestParam("dataSourceName") String name){
		return dataSourceService.deleteDataSource(uc, name);
	}
}
