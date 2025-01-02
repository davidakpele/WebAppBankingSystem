package com.pesco.authentication.repositories;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.pesco.authentication.models.UserAttempt;

@Repository
public interface UserAttemptRepository extends JpaRepository<UserAttempt, Long> {

    List<UserAttempt> findByTimestampAfter(Instant minus);

    @Query("SELECT b FROM UserAttempt b WHERE b.userId=:id")
    UserAttempt findByUserId(Long id);

}
