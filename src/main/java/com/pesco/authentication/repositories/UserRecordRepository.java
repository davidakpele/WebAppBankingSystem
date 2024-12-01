package com.pesco.authentication.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.pesco.authentication.models.UserRecord;

@Repository
public interface UserRecordRepository extends JpaRepository<UserRecord, Long>{
    Optional<UserRecord> findByUserId(Long userId);

    // Query to check if the user account is locked based on user ID
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM UserRecord r WHERE r.user.id = :userId AND r.locked = true")
    boolean isUserAccountLocked(@Param("userId") Long userId);

    // Query to check if the user account is blocked based on user ID
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM UserRecord r WHERE r.user.id = :userId AND r.is_blocked = true")
    boolean isUserAccountBlocked(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM UserRecord r WHERE r.user.id IN :ids")
    void deleteUserRecordByIds(List<Long> ids);
}
