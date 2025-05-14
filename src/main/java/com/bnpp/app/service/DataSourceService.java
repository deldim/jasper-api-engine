package com.bnpp.app.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.bnpp.app.dao.DynamicDataSourceManager;
import com.bnpp.app.model.DataSource;
import com.bnpp.app.repository.DataSourceRepository;
import com.bnpp.app.shared.ApiResponse;

@Service
public class DataSourceService {

	@Autowired
	DataSourceRepository dataSourceRepository;

	public ResponseEntity<ApiResponse<Object>> getDataSourcesNames(String uc) {
		return ResponseEntity.ok(new ApiResponse<>(true, "Request executed successfully.", dataSourceRepository.findByUc(uc)));
	}

	public ResponseEntity<ApiResponse<Object>> getDataSourcesDetails(String uc) {
		return ResponseEntity.ok(new ApiResponse<>(true, "Request executed successfully.", dataSourceRepository.findAllByUc(uc)));
	}
	
	public ResponseEntity<ApiResponse<Object>> addDataSource(String name, String hostName, String port, String dbName, String uc) {
		String connectionString=hostName+":"+port+"/"+dbName+"@root@jaimelestomates?1";
		Boolean  isConnected = DynamicDataSourceManager.testConnection(connectionString);
		if(!isConnected) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to connect to DB using provided details.", null));
		}
		dataSourceRepository.save(new DataSource(name, hostName,port,dbName,uc));
		return ResponseEntity.ok(new ApiResponse<>(true, "Data source successfully added to UC: "+uc, null));
	}

	public ResponseEntity<ApiResponse<Object>> deleteDataSource(String uc, String name){
		dataSourceRepository.deleteByUcAndName(uc, name);
		return ResponseEntity.ok(new ApiResponse<>(true, "Data source \""+name+"\" removed successfully from UC: "+uc+".", null));

	}

	public Optional<DataSource> getDataSource(String uc, String name){
		return dataSourceRepository.findByUcAndName(uc, name);
	}
}
