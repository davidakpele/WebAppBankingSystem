package com.example.deposit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.deposit.models.BeneficiaryDairy;

@Repository
public interface BeneficiaryDairyRepository extends JpaRepository<BeneficiaryDairy, Long>{

    Optional<BeneficiaryDairy> findByUserId(Long userId);

    @Query("SELECT b FROM BeneficiaryDairy b WHERE b.userId=:userId")
    List<BeneficiaryDairy> findAllUserBeneficiaryByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM BeneficiaryDairy b WHERE b.id IN :ids")
    void deleteUserBeneficiaryByIds(List<Long> ids);
}
