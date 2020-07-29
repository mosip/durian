package io.mosip.datashare.api;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "io.mosip.datashare.*",
		"io.mosip.kernel.auth.*" })
public class DataShareApiApplication {
	/**
	 * The main method. s
	 * 
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(DataShareApiApplication.class, args);
	}
}
