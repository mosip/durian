package io.mosip.datashare.util;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.datashare.constant.ApiName;
import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.datashare.constant.LoggerFileConstant;
import io.mosip.datashare.dto.CryptomanagerRequestDto;
import io.mosip.datashare.dto.CryptomanagerResponseDto;
import io.mosip.datashare.exception.ApiNotAccessibleException;
import io.mosip.datashare.exception.DataEncryptionFailureException;
import io.mosip.datashare.logger.DataShareLogger;
import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;


/**
 * The Class EncryptionUtil.
 */
@Component
public class EncryptionUtil {

	/** The application id. */
	@Value("${data.share.application.id}")
	private String applicationId;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.data.share.datetime.pattern";

	/** The env. */
	@Autowired
	private Environment env;

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	@Value("${mosip.kernel.data-key-splitter}")
	private String KEY_SPLITTER;

	@Autowired
	private KeyGenerator keyGenerator;

	@Autowired
	private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;

	@Autowired
	private RestUtil restUtil;

	/** The Constant IO_EXCEPTION. */
	private static final String IO_EXCEPTION = "Exception while reading packet inputStream";

	/** The Constant DATE_TIME_EXCEPTION. */
	private static final String DATE_TIME_EXCEPTION = "Error while parsing packet timestamp";

	private static final Logger LOGGER = DataShareLogger.getLogger(EncryptionUtil.class);

	/**
	 * Encrypt data.
	 *
	 * @param filedata the filedata
	 * @param refId    the ref id
	 * @return the byte[]
	 */
	public byte[] encryptData(byte[] filedata, String refId) {
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(),
				LoggerFileConstant.SUBSCRIBERID.toString(), "EncryptionUtil::encryptData()::entry");
		// TODO use input stream
		String dataToBeEncrypted;
		byte[] encryptedPacket = null;
		try {
			dataToBeEncrypted = CryptoUtil.encodeBase64(filedata);
			CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
			RequestWrapper<CryptomanagerRequestDto> request = new RequestWrapper<>();
			cryptomanagerRequestDto.setApplicationId(applicationId);
			cryptomanagerRequestDto.setData(dataToBeEncrypted);
			cryptomanagerRequestDto.setReferenceId(refId);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);

			request.setRequest(cryptomanagerRequestDto);
			cryptomanagerRequestDto.setTimeStamp(localdatetime);
			String response = restUtil.postApi(ApiName.CRYPTOMANAGER_ENCRYPT, null, "", "",
					MediaType.APPLICATION_JSON, request, String.class);

			CryptomanagerResponseDto responseObject = mapper.readValue(response,
					CryptomanagerResponseDto.class);

			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new DataEncryptionFailureException(error.getMessage());
			}
			encryptedPacket = responseObject.getResponse().getData().getBytes();
			LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(), refId,
					"Encryption done successfully");
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(),
					LoggerFileConstant.SUBSCRIBERID.toString(), "EncryptionUtil::encryptData()::exit");
		} catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(),
					LoggerFileConstant.SUBSCRIBERID.toString(),
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(IO_EXCEPTION, e);
		} catch (DateTimeParseException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(),
					LoggerFileConstant.SUBSCRIBERID.toString(),
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(DATE_TIME_EXCEPTION);
		} catch (Exception e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(),
					LoggerFileConstant.SUBSCRIBERID.toString(),
					"EncryptionUtil::encryptData():: error with error message" + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new DataEncryptionFailureException(e);
			}

		}
		return encryptedPacket;
    	
    }

	public byte[] encryptDataUsingKey(byte[] data, byte[] encryptionKey) {
		// supports larger key lengths, Not required to specified in java 9
		Security.setProperty("crypto.policy", "unlimited");
		final SecretKey sessionKey = keyGenerator.getSymmetricKey();
		final byte[] cipherText = cryptoCore.symmetricEncrypt(sessionKey, data, null);
		PublicKey publicKey = null;
		try {
			publicKey = KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(CryptoUtil.decodeBase64(new String(encryptionKey))));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new DataEncryptionFailureException(
					DataUtilityErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorMessage(),
					e);
		}
		byte[] encryptedSessionKey = cryptoCore.asymmetricEncrypt(publicKey, sessionKey.getEncoded());
		return CryptoUtil.combineByteArray(cipherText, encryptedSessionKey, KEY_SPLITTER);
	}

}
