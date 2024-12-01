package com.example.deposit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.deposit.models.BlackListedWallet;

@Repository
public interface BlackListedWalletRepository extends JpaRepository<BlackListedWallet, Long> {

     @Query("SELECT COUNT(b) > 0 FROM BlackListedWallet b WHERE b.walletId=:walletId")
    boolean findByWalletId(@Param("walletId") Long walletId);
}
