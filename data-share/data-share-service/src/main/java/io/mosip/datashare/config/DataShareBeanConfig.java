package io.mosip.datashare.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.mosip.commons.khazana.impl.S3Adapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.datashare.util.RestUtil;


@Configuration
@EnableCaching
@EnableScheduling
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

	@Bean
	@Primary
	public ObjectMapper getObjectMapper() {
		ObjectMapper mapper = new ObjectMapper().registerModule(new AfterburnerModule()).registerModule(new JavaTimeModule());
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		return mapper;
	}

}
