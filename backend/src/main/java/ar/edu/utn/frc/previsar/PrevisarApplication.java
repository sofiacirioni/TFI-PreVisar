package ar.edu.utn.frc.previsar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PrevisarApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrevisarApplication.class, args);
	}

}
