package com.estatex.repository;

import com.estatex.model.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByCustomerIdOrderByIdDesc(Long customerId);

    List<Inquiry> findByPropertyIdOrderByIdDesc(Long propertyId);

    List<Inquiry> findByPropertyIdInOrderByIdDesc(List<Long> propertyIds);

    List<Inquiry> findAllByOrderByIdDesc();

    long countByPropertyIdInAndStatus(List<Long> propertyIds, String status);

    long countByCustomerIdAndStatus(Long customerId, String status);
}
