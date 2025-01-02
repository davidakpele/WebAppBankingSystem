package com.example.deposit.enums;

public enum BannedReasons {
    /**
     * User has exhibited suspicious activity, such as unusual login patterns or
     * transactions.
    */
    SUSPICIOUS_ACTIVITY("This user banned due to suspicious activity, Deposited and fast withdraw in less than one minute"),
    /**
     * User has violated the platform's terms of service or community guidelines.
     */
    TERMS_OF_SERVICE_VIOLATION("This user banned for violating platform rules"),

    /**
     * User has engaged in fraudulent or deceptive behavior, such as phishing or
     * scamming.
     */
    FRAUDULENT_ACTIVITY("This user has engaged in fraudulent or deceptive behavior"),
    /**
     * User has harassed or bullied other users on the platform.
     */
    HARASSMENT_OR_BULLYING("This user has harassed or bullied other users on the platform"),

    /**
     * User has posted or shared explicit or inappropriate content.
     */
    INAPPROPRIATE_CONTENT("This user has posted or shared explicit or inappropriate content"),

    /**
     * User has attempted to manipulate or exploit the platform's systems or algorithms.
     */
    PLATFORM_MANIPULATION("This user has attempted to manipulate or exploit the platform's systems or algorithms"),

    /**
     * User has failed to verify their identity or provide accurate information.
     */
    IDENTITY_VERIFICATION_FAILURE("This user has failed to verify their identity or provide accurate information"),

    /**
     * User has engaged in spamming or other forms of unwanted solicitation.
     */
    SPAMMING_OR_SOLICITATION("This user has engaged in spamming or other forms of unwanted solicitation"),

    /**
     * User has been reported by multiple other users for violating platform policies.
     */
    MULTIPLE_USER_REPORTS("This user has been reported by multiple other users for violating platform policies"),

    /**
     * User has been banned by a moderator or administrator.
     */
    MODERATOR_ACTION("This user has been banned by a moderator or administrator");

    private final String description;

    BannedReasons(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
