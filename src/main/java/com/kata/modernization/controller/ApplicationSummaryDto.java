package com.kata.modernization.controller;

public class ApplicationSummaryDto {
    private String applicationId;
    private String name;
    private String status;

    public ApplicationSummaryDto(String applicationId, String name, String status) {
        this.applicationId = applicationId;
        this.name = name;
        this.status = status;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
