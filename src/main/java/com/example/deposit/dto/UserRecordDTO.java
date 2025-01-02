package com.example.deposit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRecordDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String gender;
    private boolean is_transfer_pin;
    private boolean locked;
    private LocalDateTime lockedAt;
    private String referralCode;
    private boolean isBlocked;
    private Long blockedDuration;
    private String blockedUntil;
    private String blockedReason;
    private String totalRefs;
    private String notifications;
    private String referralLink;
    private String photo;
}