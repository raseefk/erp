package com.levanto.flooring.repository;
import com.levanto.flooring.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    org.springframework.data.domain.Page<com.levanto.flooring.projection.EmployeeSummary> findByActiveTrueOrderByNameAsc(org.springframework.data.domain.Pageable p);

    @org.springframework.data.jpa.repository.Query("SELECT e.id as id, e.employeeCode as employeeCode, e.name as name, e.email as email, e.phone as phone, e.designation as designation, e.monthlySalary as monthlySalary, e.joiningDate as joiningDate FROM Employee e " +
           "WHERE (:q IS NULL OR :q = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    org.springframework.data.domain.Page<com.levanto.flooring.projection.EmployeeSummary> searchSummary(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable p);


    java.util.List<Employee> findByActiveTrueOrderByNameAsc();
    java.util.Optional<Employee> findTopByOrderByIdDesc();
}
