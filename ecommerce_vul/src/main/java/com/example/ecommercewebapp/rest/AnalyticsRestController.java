package com.example.ecommercewebapp.rest;

import com.example.ecommercewebapp.dto.JointProbabilityDto;
import com.example.ecommercewebapp.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsRestController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/product-city-joint")
    public List<JointProbabilityDto> getProductCityJoint() {
        return analyticsService.computeProductCityJointProbability();
    }
}