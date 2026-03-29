package com.proximityservice.business.controller;

import com.proximityservice.business.service.BusinessService;
import com.proximityservice.common.dto.BusinessCreateRequest;
import com.proximityservice.common.dto.BusinessDto;
import com.proximityservice.common.dto.BusinessUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BusinessDto getBusiness(@PathVariable Long id) {
        return businessService.getBusiness(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessDto createBusiness(@RequestBody BusinessCreateRequest request) {
        return businessService.createBusiness(request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BusinessDto updateBusiness(@PathVariable Long id, @RequestBody BusinessUpdateRequest request) {
        return businessService.updateBusiness(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBusiness(@PathVariable Long id) {
        businessService.deleteBusiness(id);
    }
}
