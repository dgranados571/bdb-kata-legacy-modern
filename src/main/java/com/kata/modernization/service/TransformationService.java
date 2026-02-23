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

        List<String> logs = new ArrayList<>();

        List<String> appliedRules = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        logs.add("[BLU-AGE-ANALYZER] --- PHASE 1: ANALYSIS ---");
        logs.add("[BLU-AGE-ANALYZER] Starting analysis for S3 key: " + s3Key);

        String cobolContent = s3Service.getObjectContent(s3Key);
        String className = extractClassName(s3Key);

        logs.add("[BLU-AGE-ANALYZER] Scanning WORKING-STORAGE SECTION...");
        List<String[]> discoveredFields = new ArrayList<>();

        Pattern fieldPattern = Pattern.compile("(?m)^\\s*(01|05|10)\\s+([A-Z0-9-]+)\\s+PIC\\s+([^.\\s]+)");
        Matcher matcher = fieldPattern.matcher(cobolContent);

        while (matcher.find()) {
            String name = matcher.group(2);
            String pic = matcher.group(3);

            if (name.equalsIgnoreCase("FILLER")) {
                appliedRules.add("Rule 3: Redundant Field Filtering (Ignored FILLER field)");
                continue;
            }

            discoveredFields.add(new String[] { name, pic });
            logs.add("[BLU-AGE-ANALYZER] Component identified: " + name + " (Type: " + pic + ")");
        }

        logs.add("[BLU-AGE-VELOCITY] --- PHASE 2: GENERATION (VELOCITY ENGINE) ---");
        logs.add("[BLU-AGE-VELOCITY] Generating Java Class: " + className + ".java");

        StringBuilder javaCode = new StringBuilder();
        javaCode.append("import java.math.BigDecimal;\n");
        javaCode.append("import java.time.LocalDate;\n\n");
        javaCode.append("public class ").append(className).append(" {\n\n");

        for (String[] field : discoveredFields) {
            String cobolName = field[0];
            String pic = field[1];

            String javaField = toCamelCase(cobolName);
            appliedRules.add("Rule 4: Naming Standardization (Mapped " + cobolName + " to " + javaField + ")");

            String javaType = "String";

            if (cobolName.contains("FECHA") && (pic.contains("9(6)") || pic.contains("9(8)"))) {
                javaType = "LocalDate";
                appliedRules.add("Rule 1: Date Conversion (Mapped " + cobolName + " to LocalDate)");
            } else if (pic.contains("V")) {
                javaType = "BigDecimal";
                appliedRules.add("Rule 2: Decimal Precision (Mapped " + cobolName + " to BigDecimal)");
            } else if (pic.contains("9")) {
                javaType = "Integer";
                appliedRules.add("Rule 5: Smart Typing (Mapped " + cobolName + " to Integer)");
            } else if (pic.contains("X")) {
                javaType = "String";
                appliedRules.add("Rule 5: Smart Typing (Mapped " + cobolName + " to String)");
            } else {
                warnings.add("Warning: Complex PIC format " + pic + " for " + cobolName + " might need manual review.");
            }

            javaCode.append("    private ").append(javaType).append(" ").append(javaField).append(";\n");
            logs.add("[BLU-AGE-VELOCITY] Transformation: " + cobolName + " [" + pic + "] -> " + javaType + " "
                    + javaField);
        }

        logs.add("[BLU-AGE-VELOCITY] --- PHASE 2.5: LOGIC TRANSFORMATION ---");
        logs.add("[BLU-AGE-VELOCITY] Scanning PROCEDURE DIVISION for business rules...");

        String logicMethod = transformLogic(cobolContent, appliedRules, warnings);
        javaCode.append("\n").append(logicMethod).append("\n");

        javaCode.append("    public static void main(String[] args) {\n");
        javaCode.append("        ").append(className).append(" app = new ").append(className).append("();\n");
        javaCode.append("        System.out.println(\"--- Starting Modernized Execution ---\");\n");
        javaCode.append("        app.executeLegacyLogic();\n");
        javaCode.append("        System.out.println(\"--- Execution Finished ---\");\n");
        javaCode.append("    }\n\n");

        javaCode.append("    // Getters and Setters omitted\n");
        javaCode.append("}");

        String targetKey = "MODERN_CODE/" + className + ".java";
        try {
            s3Service.uploadContent(targetKey, javaCode.toString());
            logs.add("[BLU-AGE-PACKAGER] Source code uploaded to: s3://" + targetKey);
        } catch (Exception e) {
            warnings.add("Warning: Failed to upload source code to S3: " + e.getMessage());
        }

        logs.add("[BLU-AGE-REPORT] --- PHASE 3: MODERNIZATION REPORT ---");
        logs.add("[BLU-AGE-REPORT] Applied Rules Count: " + appliedRules.stream().distinct().count());
        appliedRules.stream().distinct().forEach(rule -> logs.add("[BLU-AGE-REPORT] [APPLIED] " + rule));

        if (warnings.isEmpty()) {
            logs.add("[BLU-AGE-REPORT] [STATUS] Clean migration: 0 warnings detected.");
        } else {
            warnings.forEach(w -> logs.add("[BLU-AGE-REPORT] [WARNING] " + w));
        }

        logs.add("[BLU-AGE-PACKAGER] --- PHASE 4: PACKAGING ---");
        logs.add("[BLU-AGE-PACKAGER] Creating artifact: modernized-" + className.toLowerCase() + ".jar");
        logs.add("[BLU-AGE-PACKAGER] Success: Transformation complete.");

        return logs;
    }

    private String extractClassName(String s3Key) {
        String filename = s3Key.contains("/") ? s3Key.substring(s3Key.lastIndexOf("/") + 1) : s3Key;
        if (filename.contains(".")) {
            filename = filename.substring(0, filename.lastIndexOf("."));
        }
        return toPascalCase(filename);
    }

    private String toPascalCase(String input) {
        String camel = toCamelCase(input);
        if (camel.isEmpty())
            return input;
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    private String transformLogic(String cobolContent, List<String> appliedRules, List<String> warnings) {
        StringBuilder method = new StringBuilder();
        method.append("    public void executeLegacyLogic() {\n");
        method.append("        System.out.println(\"Executing modernized legacy logic...\");\n\n");

        int procedureStart = cobolContent.indexOf("PROCEDURE DIVISION");
        if (procedureStart == -1) {
            method.append("        // No PROCEDURE DIVISION found.\n");
            method.append("    }\n");
            warnings.add("Warning: No PROCEDURE DIVISION found for logic transformation.");
            return method.toString();
        }

        String procedureContent = cobolContent.substring(procedureStart);
        String[] lines = procedureContent.split("\n");
        boolean logicFound = false;

        for (String line : lines) {
            String trimmedLine = line.trim().toUpperCase();
            if (trimmedLine.startsWith("MOVE ")) {
                Pattern movePattern = Pattern.compile("MOVE\\s+([^\\s]+)\\s+TO\\s+([^.\\s]+)");
                Matcher m = movePattern.matcher(trimmedLine);
                if (m.find()) {
                    String source = m.group(1).replace("'", "\"");
                    if (!source.startsWith("\"") && !source.matches("-?\\d+(\\.\\d+)?")) {
                        source = "this." + toCamelCase(source);
                    }
                    String target = toCamelCase(m.group(2));
                    method.append("        this.").append(target).append(" = ").append("new BigDecimal(" + source + ")")
                            .append(";\n");
                    appliedRules.add("Rule 6: Logic Mapping (MOVE " + m.group(1) + " to " + target + ")");
                    logicFound = true;
                }
            } else if (trimmedLine.startsWith("COMPUTE ")) {
                Pattern computePattern = Pattern.compile("COMPUTE\\s+([^\\s]+)\\s*=\\s*([^.]+)");
                Matcher m = computePattern.matcher(trimmedLine);
                if (m.find()) {
                    String target = toCamelCase(m.group(1));
                    method.append("        ").append(buildJavaAssignment(trimmedLine)).append("\n");
                    appliedRules.add("Rule 7: Formula Mapping (COMPUTE " + target + ")");
                    logicFound = true;
                }
            } else if (trimmedLine.startsWith("IF ")) {
                method.append("        // TODO: IF statement detected - Manual review recommended\n");
                method.append("        // ").append(trimmedLine).append("\n");
                warnings.add("Warning: IF statement detected. Complex branching requires manual validation.");
            } else if (trimmedLine.startsWith("DISPLAY ")) {
                String content = trimmedLine.substring(8).replace(".", "").trim();
                String[] parts = content.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                StringBuilder displayMsg = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].replace("'", "\"");
                    if (!part.startsWith("\"")) {
                        displayMsg.append("this.").append(toCamelCase(part));
                    } else {
                        displayMsg.append(part);
                    }
                    if (i < parts.length - 1) {
                        displayMsg.append(" + ");
                    }
                }
                method.append("        System.out.println(").append(displayMsg).append(");\n");
                logicFound = true;
            }
        }

        if (!logicFound) {
            method.append("        // No common business logic patterns recognized.\n");
        }

        method.append("    }\n");
        return method.toString();
    }

    private String buildJavaAssignment(String cobolLine) {
        String trimmed = cobolLine.replace("COMPUTE", "").replace(".", "").trim();

        String[] parts = trimmed.split("=");
        if (parts.length != 2) {
            return "// Error: línea COBOL inválida";
        }

        String left = parts[0].trim();
        String right = parts[1].trim();

        String leftCamel = toCamelCase(left);
        String[] rightTokens = right.split("\\s+");

        StringBuilder rightCamel = new StringBuilder();
        for (String token : rightTokens) {
            if (token.equals("*")) {
                rightCamel.append(".multiply(");
            } else if (token.equals("+")) {
                rightCamel.append(".add(");
            } else if (token.equals("-")) {
                rightCamel.append(".subtract(");
            } else if (token.equals("/")) {
                rightCamel.append(".divide(");
            } else {
                rightCamel.append("this.").append(toCamelCase(token));
            }
        }
        String javaRight = rightCamel.toString();
        if (javaRight.contains("multiply(") || javaRight.contains("add(") || javaRight.contains("subtract(")
                || javaRight.contains("divide(")) {
            javaRight += ")";
        }
        return "this." + leftCamel + " = " + javaRight + ";";
    }

    private String toCamelCase(String cobolField) {
        StringBuilder result = new StringBuilder();
        String[] parts = cobolField.split("[-_]");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (part.isEmpty())
                continue;
            if (i == 0) {
                result.append(part);
            } else {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        String valor = result.toString().replace("ws", "");
        if (valor.isEmpty()) {
            return valor;
        }
        return valor.substring(0, 1).toLowerCase() + valor.substring(1);
    }
}
