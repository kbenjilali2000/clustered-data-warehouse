package com.progresssoft.fxdeals.clustereddatawarehouse.service;

import com.progresssoft.fxdeals.clustereddatawarehouse.domain.Deal;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealImportResultDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealRequestDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.repository.DealRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DealImportServiceImpl.
 *
 * These tests focus on the business behavior of the service:
 *  - Successful imports of valid deals
 *  - Duplicate detection (pre-check + DB constraint)
 *  - Business validation rules (e.g., same currency)
 *  - Handling of empty or null input lists
 */
@ExtendWith(MockitoExtension.class)
class DealImportServiceImplTest {

    @Mock
    private DealRepository dealRepository;

    private DealImportServiceImpl dealImportService;

    @BeforeEach
    void setUp() {
        // Service under test with a mocked DealRepository
        dealImportService = new DealImportServiceImpl(dealRepository);
    }

    /**
     * Helper method to create a valid DealRequestDto with a given dealUniqueId.
     */
    private DealRequestDto createValidDto(String dealId) {
        return new DealRequestDto(
                dealId,
                "USD",
                "EUR",
                OffsetDateTime.of(2025, 1, 1, 10, 15, 30, 0, ZoneOffset.UTC),
                new BigDecimal("12345.6789")
        );
    }

    @Test
    void importDeals_allValid_noDuplicates_shouldImportAll() {
        // Given
        DealRequestDto dto1 = createValidDto("D-1");
        DealRequestDto dto2 = createValidDto("D-2");
        List<DealRequestDto> input = List.of(dto1, dto2);

        // Repository says no duplicates
        when(dealRepository.existsByDealUniqueId("D-1")).thenReturn(false);
        when(dealRepository.existsByDealUniqueId("D-2")).thenReturn(false);

        // When
        DealImportResultDto result = dealImportService.importDeals(input);

        // Then
        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getImported()).isEqualTo(2);
        assertThat(result.getInvalid()).isZero();
        assertThat(result.getDuplicates()).isZero();
        assertThat(result.getErrors()).isEmpty();

        // Verify that save() was called for each valid deal
        verify(dealRepository, times(2)).save(any(Deal.class));
    }

    @Test
    void importDeals_duplicateDeal_shouldBeCountedAsDuplicateAndNotImported() {
        // Given
        DealRequestDto dto1 = createValidDto("D-1");
        DealRequestDto dto2 = createValidDto("D-1"); // same ID -> duplicate
        List<DealRequestDto> input = List.of(dto1, dto2);

        // First check: not exists, second check: exists
        when(dealRepository.existsByDealUniqueId("D-1"))
                .thenReturn(false)  // for first row
                .thenReturn(true);  // for second row

        // When
        DealImportResultDto result = dealImportService.importDeals(input);

        // Then
        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getImported()).isEqualTo(1);
        assertThat(result.getDuplicates()).isEqualTo(1);
        assertThat(result.getInvalid()).isZero();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getDealUniqueId()).isEqualTo("D-1");
        assertThat(result.getErrors().get(0).getError())
                .contains("Duplicate dealUniqueId");

        // Only one save (for the first, non-duplicate row)
        verify(dealRepository, times(1)).save(any(Deal.class));
    }

    @Test
    void importDeals_sameFromAndToCurrency_shouldBeMarkedInvalid() {
        // Given: same currency for from/to, which violates business rule
        DealRequestDto invalidDto = new DealRequestDto(
                "D-1",
                "USD",
                "USD", // invalid: same as fromCurrency
                OffsetDateTime.now(ZoneOffset.UTC),
                new BigDecimal("100.00")
        );
        List<DealRequestDto> input = List.of(invalidDto);

        // When
        DealImportResultDto result = dealImportService.importDeals(input);

        // Then
        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getImported()).isZero();
        assertThat(result.getDuplicates()).isZero();
        assertThat(result.getInvalid()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getError())
                .contains("fromCurrencyIsoCode and toCurrencyIsoCode must be different");

        // No save should be called for invalid rows
        verify(dealRepository, never()).save(any(Deal.class));
    }

    @Test
    void importDeals_dataIntegrityViolation_shouldBeCountedAsDuplicate() {
        // Given: DB throws a unique constraint violation when saving
        DealRequestDto dto = createValidDto("D-DB-1");
        List<DealRequestDto> input = List.of(dto);

        when(dealRepository.existsByDealUniqueId("D-DB-1")).thenReturn(false);
        when(dealRepository.save(any(Deal.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        // When
        DealImportResultDto result = dealImportService.importDeals(input);

        // Then
        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getImported()).isZero();
        assertThat(result.getDuplicates()).isEqualTo(1);
        assertThat(result.getInvalid()).isZero();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getError())
                .contains("Duplicate dealUniqueId detected at database level");
    }

    @Test
    void importDeals_emptyList_shouldReturnZeroCountsAndNotTouchRepository() {
        // Given
        List<DealRequestDto> input = List.of();

        // When
        DealImportResultDto result = dealImportService.importDeals(input);

        // Then
        assertThat(result.getTotalRows()).isZero();
        assertThat(result.getImported()).isZero();
        assertThat(result.getInvalid()).isZero();
        assertThat(result.getDuplicates()).isZero();
        assertThat(result.getErrors()).isEmpty();

        // Repository should never be called
        verifyNoInteractions(dealRepository);
    }

    @Test
    void importDeals_nullList_shouldReturnZeroCountsAndNotTouchRepository() {
        // When
        DealImportResultDto result = dealImportService.importDeals(null);

        // Then
        assertThat(result.getTotalRows()).isZero();
        assertThat(result.getImported()).isZero();
        assertThat(result.getInvalid()).isZero();
        assertThat(result.getDuplicates()).isZero();
        assertThat(result.getErrors()).isEmpty();

        // Repository should never be called
        verifyNoInteractions(dealRepository);
    }
}

