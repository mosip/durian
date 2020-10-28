package io.mosip.datashare.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.datashare.constant.ApiName;
import io.mosip.datashare.constant.LoggerFileConstant;
import io.mosip.datashare.dto.SignRequestDto;
import io.mosip.datashare.dto.SignResponseDto;
import io.mosip.datashare.exception.ApiNotAccessibleException;
import io.mosip.datashare.exception.SignatureException;
import io.mosip.datashare.logger.DataShareLogger;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;


/**
 * The Class DigitalSignatureUtil.
 */
@Component
public class DigitalSignatureUtil {

	/** The environment. */
	@Autowired
	private Environment environment;


	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.data.share.datetime.pattern";



	private static final Logger LOGGER = DataShareLogger.getLogger(DigitalSignatureUtil.class);

	@Autowired
	private RestUtil restUtil;

	/**
	 * Sign.
	 *
	 * @param packet the packet
	 * @return the byte[]
	 */
	public String sign(byte[] packet) {
		try {
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					LoggerFileConstant.POLICYID.toString(),
					"DigitalSignatureUtil::sign()::entry");
			String packetData = new String(packet, StandardCharsets.UTF_8);
			SignRequestDto dto = new SignRequestDto();
			dto.setData(packetData);
			RequestWrapper<SignRequestDto> request = new RequestWrapper<>();
			request.setRequest(dto);
			request.setMetadata(null);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(environment.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(environment.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);
			String responseString = restUtil.postApi(ApiName.KEYMANAGER_SIGN, null, "", "", MediaType.APPLICATION_JSON,
					request, String.class);

			SignResponseDto responseObject = mapper.readValue(responseString, SignResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new SignatureException(error.getMessage());
			}
			String signedData = responseObject.getResponse().getSignature();
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					LoggerFileConstant.POLICYID.toString(), "DigitalSignatureUtil::sign()::exit");
			return signedData;
		} catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					LoggerFileConstant.POLICYID.toString(),
					"DigitalSignatureUtil::sign():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new SignatureException(e);
		} catch (Exception e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					LoggerFileConstant.POLICYID.toString(),
					"DigitalSignatureUtil::sign():: error with error message" + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new SignatureException(e);
			}

		}

	}

}
