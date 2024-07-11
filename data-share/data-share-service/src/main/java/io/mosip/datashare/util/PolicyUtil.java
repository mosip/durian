package io.mosip.datashare.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.mosip.datashare.dto.DataShareDto;
import io.mosip.datashare.exception.StaticDataShareException;
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

	/** The static data share policy used for sharing the data. */
	private static final String STATIC_DATA_SHARE_POLICY = "mosip.data.share.static.share.policy";

	/** The static data share policy used for sharing the data. */
	private static final String STATIC_DATA_SHARE_POLICY_ID = "mosip.data.share.static.share.policyid";

	/** The static data share policy used for sharing the data. */
	private static final String STATIC_DATA_SHARE_SUBSCRIBER_ID = "mosip.data.share.static.share.subscriberid";

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
			String staticPolicyId = env.getProperty(STATIC_DATA_SHARE_POLICY_ID);
			if (staticPolicyId == null)
				throw new StaticDataShareException("Please configure the static data share policy Id");

			String staticSubscriberId = env.getProperty(STATIC_DATA_SHARE_SUBSCRIBER_ID);
			if (staticSubscriberId == null)
				throw new StaticDataShareException("Please configure the static data share subscriber Id");

			if (!staticPolicyId.equals(policyId) || !staticSubscriberId.equals(subscriberId))
				throw new StaticDataShareException("Either Policy Id or Subscriber Id not matching with configured in system");

			String staticSharePolicy = env.getProperty(STATIC_DATA_SHARE_POLICY);
			if (staticSharePolicy == null)
				throw new StaticDataShareException("Please configure the static data share policy");

			DataShareDto dataShareDto = mapper.readValue(staticSharePolicy, DataShareDto.class);
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
					"PolicyUtil::getStaticDataSharePolicy()::exit");
			return dataShareDto;
		} catch (Exception e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					policyId,
					"PolicyUtil::getStaticDataSharePolicy():: error with error message" + ExceptionUtils.getStackTrace(e));
			if(e instanceof StaticDataShareException)
				throw (StaticDataShareException)e;
			throw new StaticDataShareException(e);
		}
	}
}
