package com.kata.modernization.controller;

import com.kata.modernization.aws.M2OrchestratorService;
import com.kata.modernization.aws.S3Service;
import com.kata.modernization.dto.InfoTransformDto;
import com.kata.modernization.service.TransformationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public InfoTransformDto runFullPipeline(@RequestBody ModernizationRequest request) {
        try {
            String appName = request.getAppName();
            String cobolPath = request.getCobolPath();
            System.out.println("RunFullPipeline: " + appName + " | " + cobolPath);
            InfoTransformDto infoTransformDto = transformationService.transformCobolToJava(cobolPath);
            return infoTransformDto;
        } catch (Exception e) {
            throw new RuntimeException("Error en el pipeline de modernización: " + e.getMessage());
        }
    }

    @GetMapping("/execute-application")
    public List<String> executeApplication(@RequestParam String key) {
        System.out.println("ExecuteApplication: " + key);
        List<String> metaDatosCobol = new ArrayList<>();
        try {
            String cobolContent = s3Service.getObjectContent(key);
            String fileName = Paths.get(key).getFileName().toString();
            Path tempDir = Files.createTempDirectory("javaexec");
            Path javaFile = Paths.get(tempDir.toString(), fileName);
            Files.writeString(javaFile, cobolContent);

            Process compile = new ProcessBuilder("javac", javaFile.toString())
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(compile.getInputStream()))) {
                metaDatosCobol.addAll(reader.lines().toList());
            }
            compile.waitFor();
            if (compile.exitValue() != 0) {
                metaDatosCobol.add("Compilation failed");
                return metaDatosCobol;
            }
            String className = javaFile.getFileName().toString().replace(".java", "");
            Process run = new ProcessBuilder("java", "-cp", tempDir.toString(), className)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()))) {
                metaDatosCobol.addAll(reader.lines().toList());
            }
            run.waitFor();
            return metaDatosCobol;

        } catch (Exception e) {
            System.err.println("Error executing application: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error executing application: " + e.getMessage());
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
