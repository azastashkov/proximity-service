package com.proximityservice.business.repository;

import com.proximityservice.common.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {}
