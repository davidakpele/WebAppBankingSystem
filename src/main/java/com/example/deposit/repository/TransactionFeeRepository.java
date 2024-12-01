package com.example.deposit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.deposit.models.TransactionFee;

@Repository
public interface TransactionFeeRepository extends JpaRepository<TransactionFee, Long> {

    TransactionFee findFirstByOrderById();

}