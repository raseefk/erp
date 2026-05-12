package com.supererp.erp.repository;

import com.supererp.erp.entity.SubcontractorRunningBillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcontractorRunningBillItemRepository extends JpaRepository<SubcontractorRunningBillItem, Long> {
    List<SubcontractorRunningBillItem> findByRunningBillIdOrderByIdAsc(Long runningBillId);
    List<SubcontractorRunningBillItem> findByBoqItemIdOrderByIdAsc(Long boqItemId);
}
