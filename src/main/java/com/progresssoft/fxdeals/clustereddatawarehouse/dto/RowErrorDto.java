package com.progresssoft.fxdeals.clustereddatawarehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a validation or parsing error for a specific row during
 * CSV parsing or deal import.
 *
 * Fields:
 *  - rowIndex: the line number in the CSV or list index (1-based)
 *  - dealUniqueId: optional — ID of the deal if available
 *  - message: the human-readable explanation of the error
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RowErrorDto {

    private int rowIndex;         // e.g., CSV row number (excluding header)
    private String dealUniqueId;  // may be null if parsing failed early
    private String message;       // detailed error description
}
