package com.kata.modernization.controller;

import com.kata.modernization.aws.M2OrchestratorService;
import com.kata.modernization.aws.S3Service;
import com.kata.modernization.service.DiscountService;
import com.kata.modernization.service.TransformationService;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.m2.model.ApplicationSummary;

import java.math.BigDecimal;
import java.util.List;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "http://localhost:3002" })
@RequestMapping("/api/modernization")
public class ModernizationController {

    private final DiscountService discountService;
    private final TransformationService transformationService;
    private final S3Service s3Service;
    private final M2OrchestratorService m2OrchestratorService;

    public ModernizationController(DiscountService discountService, TransformationService transformationService,
            S3Service s3Service, M2OrchestratorService m2OrchestratorService) {
        this.discountService = discountService;
        this.transformationService = transformationService;
        this.s3Service = s3Service;
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

    @PostMapping("/aws/register")
    public String registerApplication(
            @RequestParam String name,
            @RequestParam String description,
            @RequestBody String definitionContent) {
        try {
            return m2OrchestratorService.createM2Application(name, description, definitionContent).applicationId();
        } catch (Exception e) {
            throw new RuntimeException("Error registering application: " + e.getMessage());
        }
    }

    @PostMapping("/full-pipeline")
    public String runFullPipeline(@RequestParam String cobolPath,
            @RequestParam String appName, @RequestBody String definitionTemplate) {

        try {
            String jarPath = transformationService.transformCobolToJava(cobolPath);

            String s3Key = "apps/" + appName + "/" + jarPath;
            s3Service.uploadArtifact(s3Key, jarPath);
            String finalAppId = m2OrchestratorService
                    .createM2Application(appName, "App modernizada via Pipeline", definitionTemplate).applicationId();

            return "Pipeline completado. App ID: " + finalAppId + " | S3 Path: " + s3Key;

        } catch (Exception e) {
            throw new RuntimeException("Error en el pipeline de modernización: " + e.getMessage());
        }
    }

    @GetMapping("/aws/presigned-url")
    public String getPresignedUrl(@RequestParam String key) {
        try {
            return s3Service.getPresignedUrl(key);
        } catch (Exception e) {
            throw new RuntimeException("Error generating presigned URL: " + e.getMessage());
        }
    }
}
