package com.supererp.erp.repository;

import com.supererp.erp.entity.BoqProgressEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoqProgressEntryRepository extends JpaRepository<BoqProgressEntry, Long> {
    List<BoqProgressEntry> findByBoqItemIdOrderByProgressDateDesc(Long boqItemId);
    List<BoqProgressEntry> findByProjectIdOrderByProgressDateDesc(Long projectId);
}
