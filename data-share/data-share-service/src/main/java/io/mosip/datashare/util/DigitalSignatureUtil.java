package io.mosip.datashare.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.datashare.constant.ApiName;
import io.mosip.datashare.constant.JsonConstants;
import io.mosip.datashare.constant.LoggerFileConstant;
import io.mosip.datashare.dto.JWTSignatureRequestDto;
import io.mosip.datashare.dto.SignResponseDto;
import io.mosip.datashare.exception.ApiNotAccessibleException;
import io.mosip.datashare.exception.SignatureException;
import io.mosip.datashare.logger.DataShareLogger;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.HMACUtils2;


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

	@Value("${mosip.data.share.digest.algorithm:SHA256}")
	private String digestAlg;

	/**
	 * Sign.
	 *
	 * @param packet the packet
	 * @return the byte[]
	 */
	public String jwtSign(byte[] file, String filname, String partnerId, String creationTime, String expiryTime) {
		try {
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(), partnerId,
					"DigitalSignatureUtil::jwtSign()::entry");
			String hashData = HMACUtils2.digestAsPlainText(file);
			String digestData = CryptoUtil.encodeBase64(hashData.getBytes());

			JSONObject signatureJson = createSignatureJson(filname, partnerId, digestData, creationTime, expiryTime);
			String dataTobeSigned = mapper.writeValueAsString(signatureJson);
			String encodedData = CryptoUtil.encodeBase64(dataTobeSigned.getBytes());
			JWTSignatureRequestDto dto = new JWTSignatureRequestDto();
			dto.setDataToSign(encodedData);
			dto.setIncludeCertHash(
					environment.getProperty("mosip.data.share.includeCertificateHash", Boolean.class));
			dto.setIncludeCertificate(
					environment.getProperty("mosip.data.share.includeCertificate", Boolean.class));
			dto.setIncludePayload(environment.getProperty("mosip.data.share.includePayload", Boolean.class));
			String certificateUrl = environment.getProperty("mosip.data.share.certificateurl");
			if (StringUtils.isNotEmpty(certificateUrl)) {
				dto.setCertificateUrl(certificateUrl);
			}

			RequestWrapper<JWTSignatureRequestDto> request = new RequestWrapper<>();
			request.setRequest(dto);
			request.setMetadata(null);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(environment.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(environment.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);
			String responseString = restUtil.postApi(ApiName.KEYMANAGER_JWTSIGN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			SignResponseDto responseObject = mapper.readValue(responseString, SignResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new SignatureException(error.getMessage());
			}
			String signedData = responseObject.getResponse().getJwtSignedData();

			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(), partnerId,
					"DigitalSignatureUtil::jwtSign()::exit");
			return signedData;

		} catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(), partnerId,
					"DigitalSignatureUtil::jwtSign():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new SignatureException(e);
		} catch (Exception e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(), partnerId,
					"DigitalSignatureUtil::jwtSign():: error with error message" + ExceptionUtils.getStackTrace(e));
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

	@SuppressWarnings("unchecked")
	private JSONObject createSignatureJson(String filname, String partnerId, String digestData, String createTime,
			String expiryTime) {
		JSONObject json = new JSONObject();
		json.put(JsonConstants.FILENAME, filname);
		json.put(JsonConstants.CREATED, createTime);
		json.put(JsonConstants.EXPIRES, expiryTime);
		json.put(JsonConstants.KEYID, partnerId);
		json.put(JsonConstants.DIGESTALG, digestAlg);
		json.put(JsonConstants.DIGEST, digestData);
		return json;
	}

}
