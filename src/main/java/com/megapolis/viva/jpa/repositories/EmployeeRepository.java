package com.megapolis.viva.jpa.repositories;

import com.megapolis.viva.jpa.models.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    public List<Employee> findAllByCityAndInstitutionsIn(String city, List<Employee.InstitutionThesis> institutionThesis);
    List<Employee> findAllByIsActive(Boolean isActive);
}
