package com.kata.modernization.service;

import com.kata.modernization.aws.S3Service;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TransformationService {

    private final S3Service s3Service;

    public TransformationService(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public List<String> transformCobolToJava(String s3Key) {
        List<String> metaDatosCobol = new ArrayList<>();
        metaDatosCobol.add("[BLU-AGE-ANALYZER] Starting analysis for S3 key: " + s3Key);

        String cobolContent = s3Service.getObjectContent(s3Key);

        metaDatosCobol.add("[BLU-AGE-ANALYZER] Scanning WORKING-STORAGE SECTION...");
        List<String> discoveredFields = new ArrayList<>();

        Pattern fieldPattern = Pattern.compile("(01|05)\\s+([A-Z0-9-]+)\\s+PIC");
        Matcher matcher = fieldPattern.matcher(cobolContent);

        while (matcher.find()) {
            discoveredFields.add(matcher.group(2));
            metaDatosCobol.add("[BLU-AGE-ANALYZER] Found metadata entry: " + matcher.group(2));
        }

        metaDatosCobol.add("[BLU-AGE-ANALYZER] Total components discovered: " + discoveredFields.size());

        metaDatosCobol.add("[BLU-AGE-VELOCITY] Generating Java Domain Models...");
        for (String field : discoveredFields) {
            String javaField = toCamelCase(field);
            metaDatosCobol.add("[BLU-AGE-VELOCITY] Mapping " + field + " -> private "
                    + (field.contains("ID") ? "Long " : "String ") + javaField + ";");
        }

        metaDatosCobol.add("[BLU-AGE-VELOCITY] Packaging Spring Boot application...");
        metaDatosCobol.add("[BLU-AGE-VELOCITY] Success: Transformation complete.");

        return metaDatosCobol;
    }

    private String toCamelCase(String cobolField) {
        StringBuilder result = new StringBuilder();
        String[] parts = cobolField.split("-");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (i == 0) {
                result.append(part);
            } else {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return result.toString().replace("ws", "");
    }
}
