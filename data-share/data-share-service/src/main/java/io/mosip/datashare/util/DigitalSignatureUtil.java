package io.mosip.datashare.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

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
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils2;


/**
 * The Class DigitalSignatureUtil.
 */
@Component
public class DigitalSignatureUtil {

	private static final Logger LOGGER = DataShareLogger.getLogger(DigitalSignatureUtil.class);

	@Value("${mosip.data.share.datetime.pattern}")
	private String dateTimePattern;

	@Value("${mosip.data.share.includeCertificateHash}")
	private boolean includeCertificateHash;

	@Value("${mosip.data.share.includeCertificate}")
	private boolean includeCertificate;

	@Value("${mosip.data.share.includePayload}")
	private boolean includePayload;

	@Value("${mosip.data.share.certificateurl:}")
	private String certificateUrl;

	@Value("${mosip.data.share.digest.algorithm:SHA256}")
	private String digestAlg;

	@Autowired
	private RestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;

	private ObjectReader signRespReader;
	private DateTimeFormatter formatter;

	@PostConstruct
	private void init() {
		this.signRespReader = mapper.readerFor(SignResponseDto.class);
		this.formatter = DateTimeFormatter.ofPattern(dateTimePattern);
	}

	/**
	 * Requests a JWT signature from Keymanager.
	 *
	 * @param file         raw file bytes to hash and include in the payload digest
	 * @param filname      filename to embed in the payload
	 * @param partnerId    key identifier (kid) / partner ID
	 * @param creationTime ISO/date-time string for {@code created} claim
	 * @param expiryTime   ISO/date-time string for {@code expires} claim
	 * @return compact JWS string
	 * @throws SignatureException        if Keymanager reports an error or response is invalid
	 * @throws ApiNotAccessibleException if the API returns an HTTP error body
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
			dto.setIncludeCertHash(includeCertificateHash);
			dto.setIncludeCertificate(includeCertificate);
			dto.setIncludePayload(includePayload);
			if (StringUtils.isNotEmpty(certificateUrl)) {
				dto.setCertificateUrl(certificateUrl);
			}

			RequestWrapper<JWTSignatureRequestDto> request = new RequestWrapper<>();
			request.setRequest(dto);
			request.setMetadata(null);

			// Step 2: Generate UTC timestamp in configured pattern
			LocalDateTime nowUtc = LocalDateTime.parse(DateUtils.getUTCCurrentDateTimeString(dateTimePattern), formatter);
			request.setRequesttime(nowUtc);

			String responseString = restUtil.postApi(ApiName.KEYMANAGER_JWTSIGN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			SignResponseDto responseObject = signRespReader.readValue(responseString);			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
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
