package io.mosip.datashare.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.mosip.commons.khazana.impl.S3Adapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.datashare.util.RestUtil;


@Configuration
@EnableCaching
@EnableScheduling
@PropertySource("classpath:bootstrap.properties")
public class DataShareBeanConfig {

	@Value("${mosip.data.share.async.core-pool-size:25}")
	private int asyncCorePoolSize;

	@Value("${mosip.data.share.async.max-pool-size:50}")
	private int asyncMaxPoolSize;

	@Value("${mosip.data.share.async.queue-capacity:80}")
	private int asyncQueueCapacity;

	/**
	 * Thread pool for parallelising independent async operations within a request
	 * (e.g. encryption + JWT signing).  Sizes are tunable via properties.
	 */
	@Bean(name = "dataShareTaskExecutor")
	public TaskExecutor dataShareTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(asyncCorePoolSize);
		executor.setMaxPoolSize(asyncMaxPoolSize);
		executor.setQueueCapacity(asyncQueueCapacity);
		executor.setThreadNamePrefix("datashare-async-");
		// CallerRunsPolicy: when pool is saturated, run on the calling (Tomcat) thread
		// instead of rejecting. Gracefully degrades to sequential under extreme load.
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}

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
