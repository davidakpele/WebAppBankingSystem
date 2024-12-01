package com.example.deposit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayStackBankList {
    private int id;
    private String name;
    private String slug;
    private String code;
    private String longcode;
    private String gateway;
    private boolean pay_with_bank;
    private boolean supports_transfer;
    private boolean active;
    private String country;
    private String currency;
    private String type;
    private boolean is_deleted;
    private String createdAt;
    private String updatedAt;
}
