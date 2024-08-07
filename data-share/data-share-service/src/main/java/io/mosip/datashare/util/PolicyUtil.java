package io.mosip.datashare.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.mosip.datashare.dto.DataShareDto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.datashare.constant.ApiName;
import io.mosip.datashare.constant.LoggerFileConstant;
import io.mosip.datashare.dto.PolicyManagerResponseDto;
import io.mosip.datashare.dto.PolicyResponseDto;
import io.mosip.datashare.exception.ApiNotAccessibleException;
import io.mosip.datashare.exception.PolicyException;
import io.mosip.datashare.logger.DataShareLogger;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class PolicyUtil {



	/** The env. */
	@Autowired
	private Environment env;

	/** The rest template. */
	@Autowired
	private RestUtil restUtil;

	/** The cryptomanager encrypt url. */
	@Value("${PARTNER_POLICY}")
	private String partnerPolicyUrl;

	private static final Logger LOGGER = DataShareLogger.getLogger(PolicyUtil.class);

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	/** Defines whether static data share policy needs to be used for sharing the data*/
	@Value("${mosip.data.share.standalone.mode.enabled:false}")
	private boolean standaloneModeEnabled;

	/** The static data share policy Json used for sharing the data. */
	@Value("${mosip.data.share.static-policy.policy-json:#{null}}")
	private String staticPolicyJson;

	/** The static data share policyId used for sharing the data. */
	@Value("${mosip.data.share.static-policy.policy-id:#{null}}")
	private String staticPolicyId;

	/** The static data share subscriberId used for sharing the data. */
	@Value("${mosip.data.share.static-policy.subscriber-id:#{null}}")
		private String staticSubscriberId;

	@Cacheable(value = "partnerpolicyCache", key = "#policyId + '_' + #subscriberId")
	public PolicyResponseDto getPolicyDetail(String policyId, String subscriberId) {

		try {
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					policyId, "PolicyUtil::getPolicyDetail()::entry");
			Map<String, String> pathsegments = new HashMap<>();
			pathsegments.put("partnerId", subscriberId);
			pathsegments.put("policyId", policyId);
			String responseString = restUtil.getApi(ApiName.PARTNER_POLICY, pathsegments, String.class);
            PolicyResponseDto policyResponseDto=new PolicyResponseDto();
            PolicyManagerResponseDto responseObject = mapper.readValue(responseString,
                        PolicyManagerResponseDto.class);
            if (responseObject!=null) {
                if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
                    ServiceError error = responseObject.getErrors().get(0);
                    throw new PolicyException(error.getMessage());
                }
                policyResponseDto = responseObject.getResponse();
            }

			LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					policyId,
					"Fetched policy details successfully");
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
					"PolicyUtil::getPolicyDetail()::exit");
			return policyResponseDto;
		} catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					policyId,
					"PolicyUtil::getPolicyDetail():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new PolicyException(e);
		} catch (Exception e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					policyId,
					"PolicyUtil::getPolicyDetail():: error with error message" + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new PolicyException(e);
			}

		}

	}

	@CacheEvict(value = "partnerpolicyCache", allEntries = true)
	@Scheduled(fixedRateString = "${mosip.data.share.policy-cache.expiry-time-millisec}")
	public void emptyPartnerPolicyCache() {
		LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
				"emptyPartnerPolicyCache", "Emptying Partner Policy cache");

	}

	/**
	 * Provides static data share policy for sharing the data.
	 * @param policyId Policy Id from request
	 * @param subscriberId Subscriber Id from request
	 * @return the DataShareDto object
	 */
	public DataShareDto getStaticDataSharePolicy(String policyId, String subscriberId) {
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
				policyId, "PolicyUtil::getStaticDataSharePolicy()::entry");
		try {
			if (!policyId.equals(staticPolicyId) || !subscriberId.equals(staticSubscriberId))
				throw new PolicyException("Either Policy Id or Subscriber Id not matching with configured in system");

			DataShareDto dataShareDto = mapper.readValue(staticPolicyJson, DataShareDto.class);
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
					"PolicyUtil::getStaticDataSharePolicy()::exit");
			return dataShareDto;
		} catch (Exception e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					policyId,
					"PolicyUtil::getStaticDataSharePolicy():: error with error message" + ExceptionUtils.getStackTrace(e));
			if(e instanceof PolicyException)
				throw (PolicyException)e;
			throw new PolicyException(e);
		}
	}

	/** This method validates the properties configured for the standalone mode for the data-share application.*/
	@PostConstruct
	private void validateStandaloneDataShareProperties() {
		if(!standaloneModeEnabled) {
			LOGGER.info("Application is running in integrated mode");
			return;
		}
		LOGGER.info("Application is running in standalone mode");
		if (Objects.isNull(staticPolicyId))
			throw new PolicyException("Please configure the static data share policy Id");
		if (Objects.isNull(staticSubscriberId))
			throw new PolicyException("Please configure the static data share subscriber Id");
		if (Objects.isNull(staticPolicyJson))
			throw new PolicyException("Please configure the static data share policy");
	}
}
