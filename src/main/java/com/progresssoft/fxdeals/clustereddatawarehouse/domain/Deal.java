package com.progresssoft.fxdeals.clustereddatawarehouse.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "deals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deals_deal_unique_id", columnNames = "deal_unique_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deal_unique_id", nullable = false, updatable = false)
    private String dealUniqueId;

    @Column(name = "from_currency_iso_code", nullable = false, length = 3)
    private String fromCurrencyIsoCode;

    @Column(name = "to_currency_iso_code", nullable = false, length = 3)
    private String toCurrencyIsoCode;

    @Column(name = "deal_timestamp", nullable = false)
    private OffsetDateTime dealTimestamp;

    @Column(name = "deal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal dealAmount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
