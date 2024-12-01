package com.pesco.authentication.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.Calendar;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private Long userId;

    private Date expiryDate;

    public PasswordResetToken(String token, Long userId) {
        this.token = token;
        this.userId = userId;
        this.expiryDate = calculateExpiryDate();
    }

    private Date calculateExpiryDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, 15);
        return calendar.getTime();
    }

    public boolean isExpired() {
        return new Date().after(this.expiryDate);
    }
}
