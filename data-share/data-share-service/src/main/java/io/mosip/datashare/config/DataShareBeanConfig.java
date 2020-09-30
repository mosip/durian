package io.mosip.datashare.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.mosip.commons.khazana.impl.S3Adapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.datashare.util.RestUtil;


@Configuration
@EnableCaching
@PropertySource("classpath:bootstrap.properties")
public class DataShareBeanConfig {

	@Bean
	public ObjectStoreAdapter objectStoreAdapter() {
		return new S3Adapter();
	}

	@Bean
	public RestUtil getRestUtil() {
		return new RestUtil();
	}

}
