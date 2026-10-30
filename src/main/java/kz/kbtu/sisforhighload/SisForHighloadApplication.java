package kz.kbtu.sisforhighload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SisForHighloadApplication {

	public static void main(String[] args) {
		SpringApplication.run(SisForHighloadApplication.class, args);
	}

}
