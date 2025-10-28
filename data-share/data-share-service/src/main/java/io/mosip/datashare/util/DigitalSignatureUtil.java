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
 * Utility to obtain a JWT signature from Keymanager for a given file payload.
 * <p>Optimizations:
 * <ul>
 *   <li>Uses Spring-managed {@link ObjectMapper} with a cached {@link ObjectReader}</li>
 *   <li>Caches {@link DateTimeFormatter} to avoid repetitive instantiation</li>
 *   <li>Keeps {@link DateUtils#getUTCCurrentDateTimeString(String)} for UTC timestamp generation</li>
 * </ul>
 */
@Component
public class DigitalSignatureUtil {

	private static final Logger LOGGER = DataShareLogger.getLogger(DigitalSignatureUtil.class);

	@Value("${mosip.data.share.datetime.pattern}")
	private String dateTimePattern;

	@Value("${mosip.data.share.includeCertificateHash:false}")
	private boolean includeCertificateHash;

	@Value("${mosip.data.share.includeCertificate:false}")
	private boolean includeCertificate;

	@Value("${mosip.data.share.includePayload:false}")
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

			// 1) Compute digest
			final String hashData = HMACUtils2.digestAsPlainText(file);
			final String digestData = CryptoUtil.encodeBase64(hashData.getBytes(StandardCharsets.UTF_8));

			// 2) Build signature JSON
			final JSONObject signatureJson = createSignatureJson(filname, partnerId, digestData, creationTime, expiryTime);
			final String dataTobeSigned = mapper.writeValueAsString(signatureJson);
			final String encodedData = CryptoUtil.encodeBase64(dataTobeSigned.getBytes(StandardCharsets.UTF_8));

			// 3) Prepare request
			final JWTSignatureRequestDto dto = new JWTSignatureRequestDto();
			dto.setDataToSign(encodedData);
			dto.setIncludeCertHash(includeCertificateHash);
			dto.setIncludeCertificate(includeCertificate);
			dto.setIncludePayload(includePayload);
			if (StringUtils.isNotBlank(certificateUrl)) {
				dto.setCertificateUrl(certificateUrl);
			}

			final RequestWrapper<JWTSignatureRequestDto> request = new RequestWrapper<>();
			request.setRequest(dto);
			request.setMetadata(null);

			// Step 2: Generate UTC timestamp in configured pattern
			final String nowUtcStr = LocalDateTime.now(ZoneOffset.UTC).format(formatter);
			final LocalDateTime nowUtc = LocalDateTime.parse(nowUtcStr, formatter);
			request.setRequesttime(nowUtc);

			// 4) Call Keymanager
			final String responseString = restUtil.postApi(
					ApiName.KEYMANAGER_JWTSIGN, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			if (responseString == null) {
				throw new IOException("Response body is null");
			}

			// 5) Parse and validate
			final SignResponseDto responseObject = signRespReader.readValue(responseString);

			if (responseObject == null) {
				throw new SignatureException("Empty response from Keymanager");
			}
			if (responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				final ServiceError error = responseObject.getErrors().get(0);
				throw new SignatureException(error != null ? error.getMessage() : "Unknown signature error");
			}
			if (responseObject.getResponse() == null || responseObject.getResponse().getJwtSignedData() == null) {
				throw new SignatureException("Missing jwtSignedData in response");
			}

			final String signedData = responseObject.getResponse().getJwtSignedData();

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
			if (e.getCause() instanceof HttpClientErrorException httpClientException) {
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException httpServerException) {
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new SignatureException(e);
			}
		}
	}

	/**
	 * Builds a JSON object containing metadata and digest information
	 * for a file to be signed.
	 * <p>
	 * The resulting JSON structure contains the following fields:
	 * <ul>
	 *     <li>{@link JsonConstants#FILENAME} – the original filename of the payload</li>
	 *     <li>{@link JsonConstants#CREATED} – creation timestamp of the payload</li>
	 *     <li>{@link JsonConstants#EXPIRES} – expiration timestamp for the signature</li>
	 *     <li>{@link JsonConstants#KEYID} – identifier for the signing key (partner ID)</li>
	 *     <li>{@link JsonConstants#DIGESTALG} – digest algorithm used to compute the digest</li>
	 *     <li>{@link JsonConstants#DIGEST} – Base64-encoded digest value of the payload</li>
	 * </ul>
	 * This JSON is later serialized and sent to the Keymanager service
	 * as part of the signature request.
	 *
	 * @param filname    the name of the file being signed
	 * @param partnerId  the partner or key identifier (used as the {@code kid} claim)
	 * @param digestData Base64-encoded digest value of the file
	 * @param createTime creation timestamp string (formatted according to the expected pattern)
	 * @param expiryTime expiration timestamp string (formatted according to the expected pattern)
	 * @return a {@link JSONObject} containing the signature metadata and digest
	 */
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