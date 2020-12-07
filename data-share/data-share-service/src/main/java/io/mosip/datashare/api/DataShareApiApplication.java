package io.mosip.datashare.api;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = { "io.mosip.datashare.*",
		"${mosip.auth.adapter.impl.basepackage}", "io.mosip.kernel.core.logger.config" })
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
