package io.mosip.datashare.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.mosip.kernel.core.util.DateUtils;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import io.mosip.datashare.constant.ApiName;
import io.mosip.datashare.constant.LoggerFileConstant;
import io.mosip.datashare.dto.CryptomanagerRequestDto;
import io.mosip.datashare.dto.CryptomanagerResponseDto;
import io.mosip.datashare.exception.ApiNotAccessibleException;
import io.mosip.datashare.exception.DataEncryptionFailureException;
import io.mosip.datashare.logger.DataShareLogger;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * The Class EncryptionUtil.
 */
@Component
public class EncryptionUtil {

	/** Logger instance for structured logging. */
	private static final Logger LOGGER = DataShareLogger.getLogger(EncryptionUtil.class);

	/** Error message for IO failures during response handling. */
	private static final String IO_EXCEPTION = "Exception while reading packet inputStream";

	/** Error message for timestamp parsing failures. */
	private static final String DATE_TIME_EXCEPTION = "Error while parsing packet timestamp";

	/** Application ID to include in encryption requests (default: PARTNER). */
	@Value("${data.share.application.id:PARTNER}")
	private String applicationId;

	/** Date-time pattern for request timestamps (e.g., {@code yyyy-MM-dd'T'HH:mm:ss}). */
	@Value("${mosip.data.share.datetime.pattern}")
	private String dateTimePattern;

	/** Whether to prepend thumbprint to the encrypted payload. */
	@Value("${mosip.data.share.prependThumbprint:false}")
	private boolean prependThumbprint;

	/** REST client utility for invoking Cryptomanager APIs. */
	@Autowired
	private RestUtil restUtil;

	/** Configured Jackson {@link ObjectMapper} for JSON (provided by Spring context). */
	@Autowired
	private ObjectMapper mapper;

	/** Cached Jackson {@link ObjectReader} for {@link CryptomanagerResponseDto}. */
	private ObjectReader cryptoRespReader;

	/** Cached {@link DateTimeFormatter} for formatting/parsing UTC timestamps. */
	private DateTimeFormatter formatter;

	/**
	 * Initializes reusable, thread-safe JSON readers and date-time formatter.
	 * <p>
	 * This method is executed once after Spring dependency injection is complete.
	 */
	@PostConstruct
	private void init() {
		this.cryptoRespReader = mapper.readerFor(CryptomanagerResponseDto.class);
		this.formatter = DateTimeFormatter.ofPattern(dateTimePattern);
	}

	/**
	 * Encrypt data.
	 *
	 * @param filedata the filedata
	 * @param refId    the ref id
	 * @return the byte[]
	 */
	public byte[] encryptData(byte[] filedata, String partnerId) {
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(),
				partnerId, "EncryptionUtil::encryptData()::entry");

		String dataToBeEncrypted;
		byte[] encryptedPacket = null;
		try {


			dataToBeEncrypted = CryptoUtil.encodeBase64(filedata);
			CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
			RequestWrapper<CryptomanagerRequestDto> request = new RequestWrapper<>();
			cryptomanagerRequestDto.setApplicationId(applicationId);
			cryptomanagerRequestDto.setData(dataToBeEncrypted);
			cryptomanagerRequestDto.setReferenceId(partnerId);
			cryptomanagerRequestDto.setPrependThumbprint(prependThumbprint);
			LocalDateTime localdatetime = LocalDateTime.parse(DateUtils.getUTCCurrentDateTimeString(dateTimePattern), formatter);
			request.setRequesttime(localdatetime);

			request.setRequest(cryptomanagerRequestDto);
			cryptomanagerRequestDto.setTimeStamp(localdatetime);
			String response = restUtil.postApi(ApiName.CRYPTOMANAGER_ENCRYPT, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			CryptomanagerResponseDto responseObject = cryptoRespReader.readValue(response);

			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new DataEncryptionFailureException(error.getMessage());
			}
			encryptedPacket = responseObject.getResponse().getData().getBytes();
			LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(), partnerId,
					"Encryption done successfully");
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(),
					partnerId, "EncryptionUtil::encryptData()::exit");
		} catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(),
					partnerId,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(IO_EXCEPTION, e);
		} catch (DateTimeParseException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(),
					partnerId,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(DATE_TIME_EXCEPTION);
		} catch (Exception e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.PARTNERID.toString(),
					partnerId,
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			}
			 else {
				throw new DataEncryptionFailureException(e.getMessage());
			}

		}
		return encryptedPacket;

    }


}
