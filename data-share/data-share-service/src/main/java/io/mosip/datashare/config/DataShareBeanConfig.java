package io.mosip.datashare.config;

import java.util.Collections;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;

import io.mosip.commons.khazana.impl.SwiftAdapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.kernel.auth.adapter.config.RestTemplateInterceptor;

@Configuration
@EnableCaching
@PropertySource("classpath:bootstrap.properties")
public class DataShareBeanConfig {
	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setInterceptors(Collections.singletonList(new RestTemplateInterceptor()));
		return restTemplate;
	}

	@Bean
	public ObjectStoreAdapter objectStoreAdapter() {
		return new SwiftAdapter();
	}
}
