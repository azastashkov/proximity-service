package com.proximityservice.lbs.controller;

import com.proximityservice.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NearbySearchController.InvalidRadiusException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidRadius(NearbySearchController.InvalidRadiusException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParam(MissingServletRequestParameterException ex) {
        return new ErrorResponse("Missing required parameter: " + ex.getParameterName());
    }
}
