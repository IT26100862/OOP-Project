package com.estatex.repository;

import com.estatex.model.FinancialBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialBreakdownRepository extends JpaRepository<FinancialBreakdown, Long> {

    List<FinancialBreakdown> findByCustomerIdOrderByIdDesc(Long customerId);

    List<FinancialBreakdown> findByPropertyIdOrderByIdDesc(Long propertyId);

    List<FinancialBreakdown> findAllByOrderByIdDesc();

    long countByCustomerId(Long customerId);
}
