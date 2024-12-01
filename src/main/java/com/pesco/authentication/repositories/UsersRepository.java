package com.pesco.authentication.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.pesco.authentication.models.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    
    Optional<Users> findByUsername(String username);

    Optional<Users> findByEmail(String email);

    @Query(value = "SELECT s1.id, s1.email, s1.password, s1.username, s1.two_factor_auth, s1.created_on, s1.updated_on, s1.enabled, s1.role, s2.id AS recordId, s2.gender, s2.locked, s2.locked_at, s2.referral_code, s2.is_blocked, s2.blocked_duration, s2.blocked_until, s2.blocked_reason, s2.total_refs, s2.notifications, s2.referral_link FROM users s1 LEFT JOIN user_record s2 ON s1.id = s2.user_id WHERE s1.id = :id", nativeQuery = true)
    Users findUserWithRecordById(@Param("id")Long id);

    @Query("SELECT u FROM Users u WHERE u.username IN :usernames")
    List<Users> findByUsernameIn(@Param("usernames") List<String> usernames);

    @Modifying
    @Query("DELETE FROM Users u WHERE u.id IN :ids")
    void deleteUserByIds(List<Long> ids);
}
