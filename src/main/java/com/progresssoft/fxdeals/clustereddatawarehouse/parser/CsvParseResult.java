package com.progresssoft.fxdeals.clustereddatawarehouse.parser;

import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealRequestDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.RowErrorDto;

import java.util.List;

/**
 * Immutable value object representing the outcome of parsing a CSV file:
 *  - parsedDeals: successfully parsed DealRequestDto objects
 *  - parseErrors: row-level errors that occurred during parsing
 *  - totalRows: total number of *data* rows (excluding the header line),
 *               including both valid and invalid ones.
 *
 * This is separated from the service-level import result so that:
 *  - parsing concerns (format, structure) stay in the parser
 *  - business validation (duplicates, domain rules) stays in the service
 */
public record CsvParseResult(
        List<DealRequestDto> parsedDeals,
        List<RowErrorDto> parseErrors,
        int totalRows
) {
}
