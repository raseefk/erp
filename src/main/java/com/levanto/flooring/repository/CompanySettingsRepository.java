package com.levanto.flooring.repository;

import com.levanto.flooring.entity.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {
}
