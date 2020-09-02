package io.mosip.datashare.util;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

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
	private RestTemplate restTemplate;

	/** The cryptomanager encrypt url. */
	@Value("${PARTNER_POLICY}")
	private String partnerPolicyUrl;

	private static final Logger LOGGER = DataShareLogger.getLogger(PolicyUtil.class);

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;
	public PolicyResponseDto getPolicyDetail(String policyId, String subscriberId) {

		try {
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(),
					LoggerFileConstant.SUBSCRIBERID.toString(), "PolicyUtil::getPolicyDetail()::entry");
		String uri = partnerPolicyUrl;
		Map<String, String> parameters = new HashMap<>();
		parameters.put("partnerId", subscriberId);
		parameters.put("policyId", policyId);
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri);

			URI urlWithPath = builder.build(parameters);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity httpEntity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(urlWithPath.toString(), HttpMethod.GET, httpEntity,
				String.class);


			PolicyManagerResponseDto responseObject = mapper.readValue(response.getBody(),
					PolicyManagerResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new PolicyException(error.getMessage());
			}
			PolicyResponseDto policyResponseDto = responseObject.getResponse();
			LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(),
					subscriberId,
					"Fetched policy details successfully");
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(),
					LoggerFileConstant.SUBSCRIBERID.toString(), "PolicyUtil::getPolicyDetail()::exit");
			return policyResponseDto;
		} catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					LoggerFileConstant.POLICYID.toString(),
					"PolicyUtil::getPolicyDetail():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new PolicyException(e);
		} catch (Exception e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					LoggerFileConstant.POLICYID.toString(),
					"PolicyUtil::getPolicyDetail():: error with error message" + ExceptionUtils.getStackTrace(e));
			if (e instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e;
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e;
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new PolicyException(e);
			}

		}

	}


}
