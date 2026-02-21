package com.kata.modernization.controller;

import com.kata.modernization.aws.M2OrchestratorService;
import com.kata.modernization.service.DiscountService;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.m2.model.ApplicationSummary;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/modernization")
public class ModernizationController {

    private final DiscountService discountService;
    private final M2OrchestratorService m2OrchestratorService;

    public ModernizationController(DiscountService discountService, M2OrchestratorService m2OrchestratorService) {
        this.discountService = discountService;
        this.m2OrchestratorService = m2OrchestratorService;
    }

    @GetMapping("/test-logic")
    public DiscountService.DiscountResult testLogic(
            @RequestParam Long id,
            @RequestParam String name,
            @RequestParam BigDecimal balance) {
        return discountService.calculateDiscount(id, name, balance);
    }

    @GetMapping("/aws/applications")
    public List<ApplicationSummary> listApplications() {
        try {
            return m2OrchestratorService.listM2Applications();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching M2 applications: " + e.getMessage());
        }
    }
}
