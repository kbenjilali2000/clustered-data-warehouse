package com.progresssoft.fxdeals.clustereddatawarehouse.repository;

import com.progresssoft.fxdeals.clustereddatawarehouse.domain.Deal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long> {

    Optional<Deal> findByDealUniqueId(String dealUniqueId);

    boolean existsByDealUniqueId(String dealUniqueId);
}
