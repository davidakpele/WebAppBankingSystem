package com.example.deposit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.deposit.models.Revenue;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {

    Revenue findFirstByOrderById();
}