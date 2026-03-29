package com.proximityservice.lbs.controller;

import com.proximityservice.common.dto.BusinessDto;
import com.proximityservice.common.dto.NearbySearchResponse;
import com.proximityservice.lbs.service.NearbySearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NearbySearchController.class)
class NearbySearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NearbySearchService searchService;

    private BusinessDto sampleDto() {
        return new BusinessDto(1L, "Test Business", "desc", "123 Main St",
            "SF", "CA", "US", "94102",
            37.7800, -122.4100, "restaurant", "415-555-0100", "http://test.com", 0.85);
    }

    @Test
    void searchNearby_validRequest_returns200() throws Exception {
        NearbySearchResponse response = new NearbySearchResponse(List.of(sampleDto()), 1);
        when(searchService.searchNearby(37.7749, -122.4194, 5.0)).thenReturn(response);

        mockMvc.perform(get("/v1/search/nearby")
                .param("latitude", "37.7749")
                .param("longitude", "-122.4194")
                .param("radius", "5.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.businesses[0].name").value("Test Business"));
    }

    @Test
    void searchNearby_defaultRadius_uses5km() throws Exception {
        NearbySearchResponse response = new NearbySearchResponse(List.of(), 0);
        when(searchService.searchNearby(37.7749, -122.4194, 5.0)).thenReturn(response);

        mockMvc.perform(get("/v1/search/nearby")
                .param("latitude", "37.7749")
                .param("longitude", "-122.4194"))
            .andExpect(status().isOk());

        verify(searchService).searchNearby(eq(37.7749), eq(-122.4194), eq(5.0));
    }

    @Test
    void searchNearby_missingLatitude_returns400() throws Exception {
        mockMvc.perform(get("/v1/search/nearby")
                .param("longitude", "-122.4194"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Missing required parameter: latitude"));
    }

    @Test
    void searchNearby_invalidRadius_returns400() throws Exception {
        mockMvc.perform(get("/v1/search/nearby")
                .param("latitude", "37.7749")
                .param("longitude", "-122.4194")
                .param("radius", "3.0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid radius. Must be one of: 0.5, 1, 2, 5, 20"));
    }
}
