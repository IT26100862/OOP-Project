package com.estatex.repository;

import com.estatex.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPropertyIdOrderByIdDesc(Long propertyId);

    List<Review> findByCustomerIdOrderByIdDesc(Long customerId);

    List<Review> findAllByOrderByIdDesc();

    long countByPropertyId(Long propertyId);

    long countByCustomerId(Long customerId);
}
