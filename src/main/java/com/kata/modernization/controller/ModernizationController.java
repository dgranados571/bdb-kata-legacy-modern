package com.kata.modernization.controller;

import com.kata.modernization.aws.M2OrchestratorService;
import com.kata.modernization.aws.S3Service;
import com.kata.modernization.service.TransformationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "http://localhost:3002" })
@RequestMapping("/api/modernization")
public class ModernizationController {

    private final TransformationService transformationService;
    private final S3Service s3Service;
    private final M2OrchestratorService m2OrchestratorService;

    @Value("classpath:app-definition.json")
    private Resource appDefinitionResource;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public ModernizationController(TransformationService transformationService, S3Service s3Service,
            M2OrchestratorService m2OrchestratorService) {
        this.transformationService = transformationService;
        this.s3Service = s3Service;
        this.m2OrchestratorService = m2OrchestratorService;
    }

    @GetMapping("/aws/presigned-url")
    public String getPresignedUrl(@RequestParam String key) {
        try {
            return s3Service.getPresignedUrl(key);
        } catch (Exception e) {
            throw new RuntimeException("Error generating presigned URL: " + e.getMessage());
        }
    }

    @PostMapping("/full-pipeline")
    public List<String> runFullPipeline(@RequestBody ModernizationRequest request) {
        List<String> metaDatosCobol = new ArrayList<>();
        try {
            String appName = request.getAppName();
            String cobolPath = request.getCobolPath();
            System.out.println("RunFullPipeline: " + appName + " | " + cobolPath);
            metaDatosCobol = transformationService.transformCobolToJava(cobolPath);
            return metaDatosCobol;
        } catch (Exception e) {
            throw new RuntimeException("Error en el pipeline de modernización: " + e.getMessage());
        }
    }

    @GetMapping("/aws/applications")
    public java.util.List<ApplicationSummaryDto> listApplications() {
        try {
            return m2OrchestratorService.listM2Applications().stream()
                    .map(app -> new ApplicationSummaryDto(
                            app.applicationId(),
                            app.name(),
                            app.status().toString()))
                    .toList();
        } catch (Exception e) {
            System.err.println("Error fetching M2 applications: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error fetching M2 applications: " + e.getMessage());
        }
    }

    @GetMapping("/aws/whoami")
    public String whoAmI() {
        return m2OrchestratorService.getCallerIdentity();
    }

    @GetMapping("/aws/s3-ls")
    public java.util.List<String> listS3(@RequestParam String prefix) {
        return s3Service.listObjects(prefix);
    }

}
