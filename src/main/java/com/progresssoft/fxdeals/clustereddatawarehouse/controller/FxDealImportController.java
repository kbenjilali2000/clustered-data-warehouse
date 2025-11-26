package com.progresssoft.fxdeals.clustereddatawarehouse.controller;

import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealImportResultDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealRequestDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.RowErrorDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.parser.CsvDealParser;
import com.progresssoft.fxdeals.clustereddatawarehouse.parser.CsvParseResult;
import com.progresssoft.fxdeals.clustereddatawarehouse.service.DealImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * REST controller exposing the FX deals import endpoints.
 * Supported Formats:
 *  - JSON array of DealRequestDto (/import)
 *  - CSV file uploaded as multipart/form-data (/import/csv)
 * Both formats delegate to DealImportService for business logic,
 * validation, duplicate detection, and persistence.
 */
@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
@Slf4j
public class FxDealImportController {

    private final DealImportService dealImportService;
    private final CsvDealParser csvDealParser;

    /**
     * Import deals from a JSON array of DealRequestDto.
     * Example:
     *     POST /api/deals/import
     *     Body: [{
     *        "dealUniqueId": "D-10001",
     *        "fromCurrencyIsoCode": "USD",
     *        "toCurrencyIsoCode": "EUR",
     *        "dealTimestamp": "2025-01-01T10:15:30Z",
     *        "dealAmount": 12345.67
     *      }]
     * @param requests list of deal requests to import
     * @return summary of the import with counts and per-row errors
     */
    @PostMapping("/import")
    public DealImportResultDto importDeals(@RequestBody List<DealRequestDto> requests) {
        log.info("Received JSON batch with {} FX deals", (requests != null ? requests.size() : 0));
        return dealImportService.importDeals(requests);
    }
    /**
     * Import deals from a CSV file.
     * Endpoint:
     *   POST /api/deals/import/csv
     *   Content-Type: multipart/form-data
     *   Form field: "file"
     * Expected CSV Header:
     *   dealUniqueId,fromCurrencyIsoCode,toCurrencyIsoCode,dealTimestamp,dealAmount
     * Notes:
     *  - CSV rows are converted into DealRequestDto objects.
     *  - Business validation, duplicate checks, and persistence
     *    are handled by DealImportService.
     *
     * @param file uploaded CSV file containing FX deals
     * @return summary of import (valid, invalid, duplicates, errors)
     */
    @PostMapping("/import/csv")
    public DealImportResultDto importDealsFromCsv(@RequestParam("file") MultipartFile file) {
        log.info("Received CSV file for FX deals import: name='{}', size={} bytes",
                file.getOriginalFilename(), file.getSize());

        // 1) Parse CSV -> valid deals + parse-time errors + total rows
        CsvParseResult parseResult = csvDealParser.parse(file);
        log.info("Parsed {} valid deals from uploaded CSV (totalRows={}, parseErrors={})",
                parseResult.parsedDeals().size(),
                parseResult.totalRows(),
                parseResult.parseErrors().size());

        // 2) Import the valid parsed deals using the same service used for JSON
        DealImportResultDto serviceResult = dealImportService.importDeals(parseResult.parsedDeals());

        // 3) Merge results:
        //    - totalRows = all CSV data rows (even those that failed parsing)
        //    - invalid = parseErrors + service-level invalid
        //    - errors = parseErrors + service-level errors
        int combinedInvalid = serviceResult.getInvalid() + parseResult.parseErrors().size();

        List<RowErrorDto> combinedErrors = new ArrayList<>(parseResult.parseErrors());

        if (serviceResult.getErrors() != null)
            combinedErrors.addAll(serviceResult.getErrors());


        DealImportResultDto finalResult = DealImportResultDto.builder()
                .totalRows(parseResult.totalRows())
                .imported(serviceResult.getImported())
                .duplicates(serviceResult.getDuplicates())
                .invalid(combinedInvalid)
                .errors(combinedErrors)
                .build();

        log.info("Finished CSV FX deals import: totalRows={}, imported={}, invalid={}, duplicates={}, errors={}",
                finalResult.getTotalRows(), finalResult.getImported(), finalResult.getInvalid(),
                finalResult.getDuplicates(), (finalResult.getErrors() != null ? finalResult.getErrors().size() : 0));

        return finalResult;
    }
}
