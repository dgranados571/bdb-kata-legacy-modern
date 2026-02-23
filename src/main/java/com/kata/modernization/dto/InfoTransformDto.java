package com.kata.modernization.dto;

import java.util.ArrayList;
import java.util.List;

public class InfoTransformDto {

    List<String> logs = new ArrayList<>();
    List<String> appliedRules = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    String targetKey;

    public InfoTransformDto() {
    }

    public InfoTransformDto(List<String> logs, List<String> appliedRules, List<String> warnings, String targetKey) {
        this.logs = logs;
        this.appliedRules = appliedRules;
        this.warnings = warnings;
        this.targetKey = targetKey;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public List<String> getAppliedRules() {
        return appliedRules;
    }

    public void setAppliedRules(List<String> appliedRules) {
        this.appliedRules = appliedRules;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(String targetKey) {
        this.targetKey = targetKey;
    }

}
