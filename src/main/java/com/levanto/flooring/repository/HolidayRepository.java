package com.levanto.flooring.repository;

import com.levanto.flooring.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByDateBetween(LocalDate startDate, LocalDate endDate);
    Optional<Holiday> findByDate(LocalDate date);
}
