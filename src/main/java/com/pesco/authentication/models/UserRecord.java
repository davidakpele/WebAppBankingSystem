package com.pesco.authentication.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.pesco.authentication.enums.UserStatus;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class UserRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private Users user;
    private String firstName;
    private String lastName;
    private String telephone;
    private String gender;
    private boolean is_transfer_pin;
    private boolean locked;
    private LocalDateTime locked_at;
    private boolean is_blocked;
    private Long blockedDuration;
    private String blockedUntil;
    private String blockedReason;
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    private String referral_code;
    private String total_refs;
    private String referral_link; 
    private String notifications;
    private String photo;
}
