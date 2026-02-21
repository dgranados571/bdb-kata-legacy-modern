package com.kata.modernization.controller;

/**
 * Request DTO for the modernization pipeline.
 */
public class ModernizationRequest {
    private String cobolPath;
    private String appName;
    private String definitionTemplate;

    // Getters and Setters
    public String getCobolPath() {
        return cobolPath;
    }

    public void setCobolPath(String cobolPath) {
        this.cobolPath = cobolPath;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDefinitionTemplate() {
        return definitionTemplate;
    }

    public void setDefinitionTemplate(String definitionTemplate) {
        this.definitionTemplate = definitionTemplate;
    }
}
