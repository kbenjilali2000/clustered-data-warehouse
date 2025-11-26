package com.progresssoft.fxdeals.clustereddatawarehouse.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Generic API error response used by the global exception handler.
 *
 * Fields:
 *  - timestamp        : when the error occurred
 *  - status           : HTTP status code
 *  - error            : short reason phrase (e.g., "Bad Request")
 *  - message          : detailed error message
 *  - path             : request path that caused the error
 *  - validationErrors : optional map of field -> validation error message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
}
