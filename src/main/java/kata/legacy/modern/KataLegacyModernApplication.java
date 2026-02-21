package kata.legacy.modern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "kata.legacy.modern", "com.kata.modernization" })
public class KataLegacyModernApplication {

	public static void main(String[] args) {
		SpringApplication.run(KataLegacyModernApplication.class, args);
	}

}
