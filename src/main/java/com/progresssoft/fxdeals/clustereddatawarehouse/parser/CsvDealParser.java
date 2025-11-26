package com.progresssoft.fxdeals.clustereddatawarehouse.parser;

import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealRequestDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.RowErrorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Component responsible for converting an uploaded CSV file into
 * a list of DealRequestDto objects, plus parse-time errors.
 *
 * Expected CSV header (exact order):
 *   dealUniqueId,fromCurrencyIsoCode,toCurrencyIsoCode,dealTimestamp,dealAmount
 */
@Component
@Slf4j
public class CsvDealParser {

    private static final String EXPECTED_HEADER =
            "dealUniqueId,fromCurrencyIsoCode,toCurrencyIsoCode,dealTimestamp,dealAmount";
    private static final int EXPECTED_COLUMN_COUNT = 5;

    /**
     * Parse the given CSV file into a list of DealRequestDto instances
     * and collect row-level parsing errors.
     *
     * @param file multipart file uploaded by the client
     * @return CsvParseResult containing parsed deals, errors and total row count
     */
    public CsvParseResult parse(MultipartFile file) {
        List<DealRequestDto> parsedDeals = new ArrayList<>();
        List<RowErrorDto> parseErrors = new ArrayList<>();
        int totalRows = 0; // data rows (excluding header)

        // Basic sanity checks on the uploaded file
        if (file == null) {
            log.warn("Received null CSV file for FX deals import. Returning empty result.");
            return new CsvParseResult(parsedDeals, parseErrors, totalRows);
        }
        if (file.isEmpty()) {
            log.warn("Received empty CSV file '{}' for FX deals import. Returning empty result.",
                    file.getOriginalFilename());
            return new CsvParseResult(parsedDeals, parseErrors, totalRows);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.warn("CSV file '{}' has no content (no header line). Returning empty result.",
                        file.getOriginalFilename());
                return new CsvParseResult(parsedDeals, parseErrors, totalRows);
            }

            // Validate header structure
            if (!isValidHeader(headerLine)) {
                String message = String.format(
                        "Invalid CSV header in file '%s'. Expected: '%s', but was: '%s'",
                        file.getOriginalFilename(), EXPECTED_HEADER, headerLine
                );
                log.error(message);
                throw new IllegalArgumentException(message);
            }

            String line;
            int lineNumber = 1; // header = line 1

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                // every non-header line counts as a potential data row
                if (line.isBlank()) {
                    log.debug("Skipping blank line {} in CSV file '{}'",
                            lineNumber, file.getOriginalFilename());
                    continue;
                }

                totalRows++;

                String[] tokens = line.split(",", -1);
                if (tokens.length < EXPECTED_COLUMN_COUNT) {
                    String msg = String.format(
                            "expected %d columns but found %d. Line content: %s",
                            EXPECTED_COLUMN_COUNT, tokens.length, line
                    );
                    log.warn("Skipping line {} in CSV file '{}': {}", lineNumber, file.getOriginalFilename(), msg);
                    parseErrors.add(new RowErrorDto(totalRows, null, msg));
                    continue;
                }

                String dealUniqueId = tokens[0].trim();
                String fromCurrency = tokens[1].trim();
                String toCurrency = tokens[2].trim();
                String timestampStr = tokens[3].trim();
                String amountStr = tokens[4].trim();

                // --- Currency code validation (length = 3, not blank) ---
                if (!isValidCurrencyCode(fromCurrency)) {
                    String msg = String.format(
                            "invalid fromCurrencyIsoCode '%s'. Expected 3-letter ISO code.",
                            fromCurrency
                    );
                    log.warn("Skipping line {} in CSV file '{}': {}",
                            lineNumber, file.getOriginalFilename(), msg);
                    parseErrors.add(new RowErrorDto(totalRows, dealUniqueId, msg));
                    continue;
                }
                if (!isValidCurrencyCode(toCurrency)) {
                    String msg = String.format(
                            "invalid toCurrencyIsoCode '%s'. Expected 3-letter ISO code.",
                            toCurrency
                    );
                    log.warn("Skipping line {} in CSV file '{}': {}",
                            lineNumber, file.getOriginalFilename(), msg);
                    parseErrors.add(new RowErrorDto(totalRows, dealUniqueId, msg));
                    continue;
                }

                fromCurrency = fromCurrency.toUpperCase();
                toCurrency = toCurrency.toUpperCase();

                // Parse timestamp
                OffsetDateTime dealTimestamp;
                if (!timestampStr.isEmpty()) {
                    try {
                        dealTimestamp = OffsetDateTime.parse(timestampStr);
                    } catch (DateTimeParseException e) {
                        String msg = String.format(
                                "invalid dealTimestamp '%s'. Error: %s",
                                timestampStr, e.getMessage()
                        );
                        log.warn("Skipping line {} in CSV file '{}': {}",
                                lineNumber, file.getOriginalFilename(), msg);
                        parseErrors.add(new RowErrorDto(totalRows, dealUniqueId, msg));
                        continue;
                    }
                } else {
                    String msg = "missing dealTimestamp value.";
                    log.warn("Skipping line {} in CSV file '{}': {}",
                            lineNumber, file.getOriginalFilename(), msg);
                    parseErrors.add(new RowErrorDto(totalRows, dealUniqueId, msg));
                    continue;
                }

                // Parse amount
                BigDecimal dealAmount;
                if (!amountStr.isEmpty()) {
                    try {
                        dealAmount = new BigDecimal(amountStr);
                    } catch (NumberFormatException e) {
                        String msg = String.format(
                                "invalid dealAmount '%s'. Error: %s",
                                amountStr, e.getMessage()
                        );
                        log.warn("Skipping line {} in CSV file '{}': {}",
                                lineNumber, file.getOriginalFilename(), msg);
                        parseErrors.add(new RowErrorDto(totalRows, dealUniqueId, msg));
                        continue;
                    }
                } else {
                    String msg = "missing dealAmount value.";
                    log.warn("Skipping line {} in CSV file '{}': {}",
                            lineNumber, file.getOriginalFilename(), msg);
                    parseErrors.add(new RowErrorDto(totalRows, dealUniqueId, msg));
                    continue;
                }

                // Create DTO and add to parsed list
                DealRequestDto dto = new DealRequestDto();
                dto.setDealUniqueId(dealUniqueId);
                dto.setFromCurrencyIsoCode(fromCurrency);
                dto.setToCurrencyIsoCode(toCurrency);
                dto.setDealTimestamp(dealTimestamp);
                dto.setDealAmount(dealAmount);

                parsedDeals.add(dto);
            }

            log.info("Successfully parsed {} deals from CSV file '{}' (totalRows={}, parseErrors={})",
                    parsedDeals.size(), file.getOriginalFilename(), totalRows, parseErrors.size());

            return new CsvParseResult(parsedDeals, parseErrors, totalRows);

        } catch (IOException e) {
            String message = String.format(
                    "I/O error while reading CSV file '%s': %s",
                    file.getOriginalFilename(), e.getMessage()
            );
            log.error(message, e);
            throw new IllegalArgumentException(message, e);
        }
    }

    private boolean isValidHeader(String headerLine) {
        if (headerLine == null) {
            return false;
        }
        String normalized = headerLine.trim();
        return EXPECTED_HEADER.equals(normalized);
    }

    private boolean isValidCurrencyCode(String code) {
        if (code == null) {
            return false;
        }
        String trimmed = code.trim();
        return !trimmed.isEmpty() && trimmed.length() == 3;
    }
}
