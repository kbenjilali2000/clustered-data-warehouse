package com.progresssoft.fxdeals.clustereddatawarehouse.service;

import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealImportResultDto;
import com.progresssoft.fxdeals.clustereddatawarehouse.dto.DealRequestDto;

import java.util.List;

/**
 * Service responsible for importing FX deals in batch.
 *
 * It processes incoming deal records one by one, applying:
 *  - Structural validation
 *  - Business validation (e.g., same currency check)
 *  - Duplicate detection
 *
 * The service guarantees that valid rows are persisted even if other rows
 * in the same batch fail (no global rollback).
 */
public interface DealImportService {

    /**
     * Imports a list of FX deal requests and returns a summary of the operation.
     *
     * @param requests list of deal request DTOs received from the client
     * @return summary of the import, including counts and per-row errors
     */
    DealImportResultDto importDeals(List<DealRequestDto> requests);
}
