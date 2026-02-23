package kata.legacy.modern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KataLegacyModernApplicationTests {

	@Test
	void contextLoads() {
		System.out.println(buildJavaAssignment("COMPUTE WS-INTERES-GANADO = WS-CAPITAL * WS-TASA-INTERES."));
		System.out.println(buildJavaAssignment("COMPUTE WS-TOTAL-CON-INTERES = WS-CAPITAL + WS-INTERES-GANADO."));
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
