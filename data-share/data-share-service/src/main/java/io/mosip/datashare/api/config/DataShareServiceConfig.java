package io.mosip.datashare.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataShareServiceConfig {

	/**
	 * DataShareServiceConfig
	 *
	 * @return the docket
	 */
	@Bean
	public OpenAPI dataShareapiBean() {
		return new OpenAPI()
				.info(new Info().title("Api Documentation")
						.description("Api Documentation")
						.version("v0.0.1")
						.license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0")));
	}
}
