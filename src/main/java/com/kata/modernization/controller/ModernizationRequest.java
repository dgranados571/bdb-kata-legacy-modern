package com.kata.modernization.controller;

public class ModernizationRequest {
    private String cobolPath;
    private String appName;

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
}
