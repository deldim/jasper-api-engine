package com.bnpp.app.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bnpp.app.model.JasperTemplate;

@Repository
public interface JasperTemplateRepository extends JpaRepository<JasperTemplate, Long> {
    List<JasperTemplate> findByUc (String uc);
    JasperTemplate findByUcAndSourceFile(String uc, String sourceFile);
    void deleteByUcAndSourceFile(String uc, String sourceFile);
    boolean existsByUcAndSourceFile(String uc, String sourceFile);
}
