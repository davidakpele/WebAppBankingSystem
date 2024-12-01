package com.example.deposit.utils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import xyz.downgoon.snowflake.Snowflake;
import org.springframework.stereotype.Component;

@Component
public class KeyCollections {
    private static final Snowflake snowflake = new Snowflake(1, 1);
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int KEY_LENGTH = 25;
    private final HttpServletRequest request;

    public KeyCollections(HttpServletRequest request) {
        this.request = request;
    }

    public static Long generateUniqueAuthorizeUserId() {
        long timestamp = System.currentTimeMillis();
        Random random = new Random();
        int randomNumber = 1000 + random.nextInt(9000);
        String combinedId = timestamp + String.valueOf(randomNumber);
        return Long.parseLong(combinedId);
    }
    /**
     * Generates a unique key.
     *
     * @return A unique key as a Base64 encoded string.
     */
    public String generateUniqueKey() {
        // Generate random bytes
        byte[] randomBytes = new byte[KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);

        // Encode bytes to Base64 and replace characters for URL safety
        String base64Encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return formatKey(base64Encoded);
    }

    /**
     * Formats the Base64 encoded key.
     *
     * @param base64Encoded The Base64 encoded string.
     * @return The formatted key.
     */
    private String formatKey(String base64Encoded) {
        return base64Encoded;
    }

    public String generateNewAddress() {
        byte[] randomBytes = new byte[25];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    public Long createSnowflakeUniqueId() {
        return snowflake.nextId();
    }

    public Long getWalletId() {
        UUID uuid = UUID.randomUUID();
        return uuid.getMostSignificantBits() & Long.MAX_VALUE;
    }

    public String getUrl() {
        String protocol = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        return protocol + "://" + serverName + ":" + serverPort + contextPath + "/api/v1";
    }

    public String getAssetUrl() {
        String protocol = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        return protocol + "://" + serverName + ":" + serverPort + contextPath;
    }

    public String generateOTP() {
        int otpLength = 6;
        Random random = new Random();

        StringBuilder otp = new StringBuilder(otpLength);

        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }

        return otp.toString();
    }

    public String generateSessionId() {
        long timestamp = System.currentTimeMillis();
        Random random = new Random();
        long randomNum = random.nextLong(1000000000000000L);
        return String.format("%014d%016d", timestamp, randomNum);
    }

}
