package com.progresssoft.fxdeals.clustereddatawarehouse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO representing a single incoming FX deal record.
 *
 * This class is used *only* for reading request payloads from the client.
 * It contains validation annotations that ensure:
 *  - All required fields are present
 *  - Currency codes follow ISO-4217 format
 *  - Amount is positive and within valid numeric precision
 *  - Timestamp is not null
 *
 * It is intentionally separated from the JPA entity "Deal" to avoid mixing
 * API-level constraints with database/domain concerns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DealRequestDto {

    @NotBlank(message = "dealUniqueId is required")
    @JsonProperty("dealUniqueId")
    private String dealUniqueId;

    @NotBlank(message = "fromCurrencyIsoCode is required")
    @Size(min = 3, max = 3, message = "fromCurrencyIsoCode must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "fromCurrencyIsoCode must be uppercase ISO currency code")
    @JsonProperty("fromCurrencyIsoCode")
    private String fromCurrencyIsoCode;

    @NotBlank(message = "toCurrencyIsoCode is required")
    @Size(min = 3, max = 3, message = "toCurrencyIsoCode must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "toCurrencyIsoCode must be uppercase ISO currency code")
    @JsonProperty("toCurrencyIsoCode")
    private String toCurrencyIsoCode;

    @NotNull(message = "dealTimestamp is required")
    @JsonProperty("dealTimestamp")
    private OffsetDateTime dealTimestamp;

    @NotNull(message = "dealAmount is required")
    @Digits(integer = 15, fraction = 4, message = "dealAmount must have up to 15 digits and 4 decimals")
    @Positive(message = "dealAmount must be positive")
    @JsonProperty("dealAmount")
    private BigDecimal dealAmount;

}
