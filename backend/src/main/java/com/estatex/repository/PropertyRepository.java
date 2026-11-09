package com.estatex.repository;

import com.estatex.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByAgentIdOrderByIdDesc(Long agentId);

    List<Property> findAllByOrderByIdDesc();

    long countByAgentId(Long agentId);

    long countByAgentIdAndStatus(Long agentId, String status);
}
