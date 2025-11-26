package com.progresssoft.fxdeals.clustereddatawarehouse.service;

import com.progresssoft.fxdeals.clustereddatawarehouse.domain.Deal;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealImportResultDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealRequestDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.RowErrorDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.repository.DealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of the DealImportService.
 *
 * Important design choices:
 *  - Processes each row individually (no @Transactional on the method)
 *    so that valid rows are saved even if others fail.
 *  - Uses database unique constraint on dealUniqueId to prevent duplicates.
 *  - Logs errors but continues processing the rest of the batch.
 *  - For both JSON and CSV imports, row-level validation errors are
 *    collected in the response instead of causing a 4xx/5xx at endpoint level.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DealImportServiceImpl implements DealImportService {

    private final DealRepository dealRepository;

    @Override
    public DealImportResultDto importDeals(List<DealRequestDto> requests) {
        int totalRows = (requests != null) ? requests.size() : 0;

        int imported = 0;
        int invalid = 0;
        int duplicates = 0;

        List<RowErrorDto> errors = new ArrayList<>();

        if (requests == null || requests.isEmpty()) {
            log.info("Received empty FX deals batch");
            return new DealImportResultDto(totalRows, imported, invalid, duplicates, errors);
        }

        log.info("Starting FX deals import with totalRows={}", totalRows);

        // Process each row individually (no batch-level transaction)
        for (int index = 0; index < requests.size(); index++) {
            int rowIndex = index + 1; // 1-based index for nicer reporting
            DealRequestDto dto = requests.get(index);
            String dealUniqueId = (dto != null ? dto.getDealUniqueId() : null);

            try {
                // 1) Additional programmatic validation (on top of any Bean Validation)
                Optional<String> validationError = validateDeal(dto);
                if (validationError.isPresent()) {
                    invalid++;
                    String errorMsg = validationError.get();
                    log.warn("Row {} (dealUniqueId={}): validation error, {}",
                            rowIndex, dealUniqueId, errorMsg);
                    errors.add(new RowErrorDto(rowIndex, dealUniqueId, errorMsg));
                    continue;
                }

                // 2) Duplicate check before hitting DB constraint
                if (dealRepository.existsByDealUniqueId(dealUniqueId)) {
                    duplicates++;
                    String errorMsg = "Duplicate dealUniqueId (already imported)";
                    log.warn("Row {} (dealUniqueId={}): {}",
                            rowIndex, dealUniqueId, errorMsg);
                    errors.add(new RowErrorDto(rowIndex, dealUniqueId, errorMsg));
                    continue;
                }

                // 3) Convert DTO to entity
                Deal deal = mapToEntity(dto);

                // 4) Persist the entity
                dealRepository.save(deal);

                imported++;
                log.info("Row {} (dealUniqueId={}): successfully imported",
                        rowIndex, dealUniqueId);

            } catch (DataIntegrityViolationException ex) {
                // Handle potential race condition duplicates at DB level
                duplicates++;
                String errorMsg = "Duplicate dealUniqueId detected at database level";
                log.warn("Row {} (dealUniqueId={}): {}", rowIndex, dealUniqueId, errorMsg);
                errors.add(new RowErrorDto(rowIndex, dealUniqueId, errorMsg));

            } catch (Exception ex) {
                // Any other unexpected error, mark row as invalid but continue
                invalid++;
                String errorMsg = "Unexpected error during import: " + ex.getMessage();
                log.error("Row {} (dealUniqueId={}): {}",
                        rowIndex, dealUniqueId, errorMsg, ex);
                errors.add(new RowErrorDto(rowIndex, dealUniqueId, errorMsg));
            }
        }

        log.info("Finished FX deals import with totalRows={}, imported={}, invalid={}, duplicates={}",
                totalRows, imported, invalid, duplicates);

        return new DealImportResultDto(totalRows, imported, invalid, duplicates, errors);
    }

    /**
     * Applies additional business validation rules that are not covered
     * by Bean Validation annotations on the DTO.
     *
     * For example:
     *  - dealUniqueId must not be empty
     *  - fromCurrencyIsoCode / toCurrencyIsoCode must be 3-letter ISO codes
     *  - fromCurrencyIsoCode should not be equal to toCurrencyIsoCode
     *  - dealTimestamp must not be null
     *  - dealAmount must be strictly positive
     *
     * @param dto incoming deal request
     * @return Optional containing an error message if validation fails, or empty if valid.
     */
    private Optional<String> validateDeal(DealRequestDto dto) {
        if (dto == null)
            return Optional.of("Deal payload is null");


        // dealUniqueId must not be blank
        if (dto.getDealUniqueId() == null || dto.getDealUniqueId().trim().isEmpty())
            return Optional.of("dealUniqueId must not be empty");

        // Currency codes must be 3-letter ISO-like codes
        if (!isValidCurrencyCode(dto.getFromCurrencyIsoCode()))
            return Optional.of("fromCurrencyIsoCode must be a 3-letter ISO code");

        if (!isValidCurrencyCode(dto.getToCurrencyIsoCode()))
            return Optional.of("toCurrencyIsoCode must be a 3-letter ISO code");


        // Business rule: ordering and counter currency must be different
        if (dto.getFromCurrencyIsoCode().equalsIgnoreCase(dto.getToCurrencyIsoCode()))
            return Optional.of("fromCurrencyIsoCode and toCurrencyIsoCode must be different");


        // Timestamp must be present
        if (dto.getDealTimestamp() == null)
            return Optional.of("dealTimestamp must not be null");


        // Amount must be present and positive
        if (dto.getDealAmount() == null)
            return Optional.of("dealAmount must not be null");

        if (dto.getDealAmount().signum() <= 0)
            return Optional.of("dealAmount must be strictly positive");


        // Could add other rules here if needed (e.g., timestamp not in far future, etc.)
        return Optional.empty();
    }

    private boolean isValidCurrencyCode(String code) {
        if (code == null) {
            return false;
        }
        String trimmed = code.trim();
        return trimmed.length() == 3 && trimmed.chars().allMatch(Character::isLetter);
    }

    /**
     * Maps a DealRequestDto into a Deal JPA entity.
     * Sets the createdAt timestamp to the current instant in UTC.
     *
     * @param dto validated deal request
     * @return Deal entity ready for persistence
     */
    private Deal mapToEntity(DealRequestDto dto) {
        return Deal.builder()
                .dealUniqueId(dto.getDealUniqueId())
                .fromCurrencyIsoCode(dto.getFromCurrencyIsoCode())
                .toCurrencyIsoCode(dto.getToCurrencyIsoCode())
                .dealTimestamp(dto.getDealTimestamp())
                .dealAmount(dto.getDealAmount())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
