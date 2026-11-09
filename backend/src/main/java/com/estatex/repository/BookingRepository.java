package com.estatex.repository;

import com.estatex.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerIdOrderByIdDesc(Long customerId);

    List<Booking> findByPropertyIdInOrderByIdDesc(Collection<Long> propertyIds);

    List<Booking> findByPropertyIdOrderByIdDesc(Long propertyId);

    List<Booking> findAllByOrderByIdDesc();

    long countByCustomerId(Long customerId);

    long countByPropertyIdIn(Collection<Long> propertyIds);
}
