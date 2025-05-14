package com.bnpp.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bnpp.app.model.DataSource;

import jakarta.transaction.Transactional;

public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
	List<DbNameOnly> findByUc(String uc);
	List<DataSource> findAllByUc(String uc);
	Optional<DataSource> findByUcAndName(String uc, String name);
	@Transactional
	void deleteByUcAndName(String uc, String name);
	public interface DbNameOnly {
		String getName();
	}
}
