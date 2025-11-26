package com.progresssoft.fxdeals.clustereddatawarehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO returned after processing a batch of FX deals.
 *
 * Fields:
 *  - totalRows   : number of rows received from the client
 *  - imported    : successfully stored deals
 *  - invalid     : rows rejected due to validation errors
 *  - duplicates  : rows skipped because dealUniqueId already exists in DB
 *  - errors      : list of detailed error messages (one per failed row)
 *
 * This structure demonstrates partial success (no rollback), which is a
 * requirement of the assignment — valid rows must be saved even if others fail.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealImportResultDto {

    private int totalRows;
    private int imported;
    private int invalid;
    private int duplicates;
    private List<RowErrorDto> errors;
}
