package com.proximityservice.business.controller;

import com.proximityservice.business.service.BusinessService;
import com.proximityservice.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessService.BusinessNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(BusinessService.BusinessNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }
}
