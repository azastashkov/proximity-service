package com.proximityservice.business.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximityservice.business.service.BusinessService;
import com.proximityservice.common.dto.BusinessCreateRequest;
import com.proximityservice.common.dto.BusinessDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessController.class)
class BusinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BusinessService businessService;

    private BusinessDto sampleDto() {
        return new BusinessDto(1L, "Test Business", "desc", "123 Main St",
            "SF", "CA", "US", "94102",
            37.7749, -122.4194, "restaurant", "415-555-0100", "http://test.com", null);
    }

    @Test
    void getBusinessById_returns200() throws Exception {
        when(businessService.getBusiness(1L)).thenReturn(sampleDto());

        mockMvc.perform(get("/v1/businesses/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Business"));
    }

    @Test
    void getBusinessById_notFound_returns404() throws Exception {
        when(businessService.getBusiness(999L))
            .thenThrow(new BusinessService.BusinessNotFoundException("Business not found: 999"));

        mockMvc.perform(get("/v1/businesses/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Business not found: 999"));
    }

    @Test
    void createBusiness_returns201() throws Exception {
        BusinessCreateRequest request = new BusinessCreateRequest(
            "Test Business", "desc", "123 Main St",
            "SF", "CA", "US", "94102",
            37.7749, -122.4194, "restaurant", "415-555-0100", "http://test.com"
        );

        when(businessService.createBusiness(any(BusinessCreateRequest.class))).thenReturn(sampleDto());

        mockMvc.perform(post("/v1/businesses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Business"));
    }

    @Test
    void deleteBusiness_returns204() throws Exception {
        doNothing().when(businessService).deleteBusiness(1L);

        mockMvc.perform(delete("/v1/businesses/1"))
            .andExpect(status().isNoContent());
    }
}
