package com.pesco.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.pesco.authentication.models.UserRecord;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    private UserDTO userDTO;
    private String photo;

    public static UserRecordDTO fromEntity(UserRecord record) {
        return UserRecordDTO.builder()
                .id(record.getId())
                .firstName(record.getFirstName())
                .lastName(record.getLastName())
                .gender(record.getGender())
                .is_transfer_pin(record.is_transfer_pin())
                .locked(record.isLocked())
                .lockedAt(record.getLocked_at())
                .referralCode(record.getReferral_code())
                .isBlocked(record.is_blocked())
                .blockedDuration(record.getBlockedDuration())
                .blockedUntil(record.getBlockedUntil())
                .blockedReason(record.getBlockedReason())
                .totalRefs(record.getTotal_refs())
                .notifications(record.getNotifications())
                .referralLink(record.getReferral_link())
                .photo(record.getPhoto())
                .build();
    }
}