package io.mosip.datashare.util;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

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
import io.mosip.datashare.constant.LoggerFileConstant;
import io.mosip.datashare.dto.CryptomanagerRequestDto;
import io.mosip.datashare.dto.CryptomanagerResponseDto;
import io.mosip.datashare.dto.KeyManagerGetCertificateResponseDto;
import io.mosip.datashare.dto.KeyManagerUploadCertificateResponseDto;
import io.mosip.datashare.dto.PartnerGetCertificateResponseDto;
import io.mosip.datashare.dto.UploadCertificateRequestDto;
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
	@Value("${data.share.application.id:DATASHARE}")
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
	public byte[] encryptData(byte[] filedata, String partnerId) {
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(),
				LoggerFileConstant.SUBSCRIBERID.toString(), "EncryptionUtil::encryptData()::entry");
		// TODO use input stream
		String dataToBeEncrypted;
		byte[] encryptedPacket = null;
		try {
			makeCertificateAvailable( partnerId);
			dataToBeEncrypted = CryptoUtil.encodeBase64(filedata);
			CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
			RequestWrapper<CryptomanagerRequestDto> request = new RequestWrapper<>();
			cryptomanagerRequestDto.setApplicationId(applicationId);
			cryptomanagerRequestDto.setData(dataToBeEncrypted);
			cryptomanagerRequestDto.setReferenceId(partnerId);
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
			LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(), partnerId,
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
			}
			 else {
				throw new DataEncryptionFailureException(e.getMessage());
			}

		}
		return encryptedPacket;
    	
    }

	private void makeCertificateAvailable(String partnerId) throws Exception {

		String getCertificateQueryParameterName="applicationId,referenceId";
		String getCertificateQueryParameterValue=applicationId+","+partnerId;

		String certificateResponse = restUtil.getApi(ApiName.KEYMANAGER_GET_CERTIFICATE, null, getCertificateQueryParameterName,
				getCertificateQueryParameterValue,  String.class);

		KeyManagerGetCertificateResponseDto certificateResponseobj = mapper.readValue(certificateResponse,
				KeyManagerGetCertificateResponseDto.class);

		if(certificateResponseobj!=null && certificateResponseobj.getResponse()!=null &&
				certificateResponseobj.getResponse().getCertificate() !=null&& !certificateResponseobj.getResponse().getCertificate().isEmpty()) {

			LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.SUBSCRIBERID.toString(), partnerId,
					"partner Certificate is available in key manager");

		}else if (certificateResponseobj != null && certificateResponseobj.getErrors() != null && !certificateResponseobj.getErrors().isEmpty()) {

			int count=0;
			for(ServiceError error:certificateResponseobj.getErrors()) {
				if (error.getErrorCode().equals("KER-KMS-002") || error.getErrorCode().equals("KER-KMS-012")
						|| error.getErrorCode().equals("KER-KMS-016") || error.getErrorCode().equals("KER-KMS-018")) {
					count++;
					Map<String, String> pathsegments = new HashMap<>();
					pathsegments.put("partnerId", partnerId);

					String partnerCertificateResponse = restUtil.getApi(ApiName.GET_PARTNER_CERTIFICATE,
							pathsegments, String.class);

					PartnerGetCertificateResponseDto partnerCertificateResponseObj=mapper.readValue(partnerCertificateResponse,
							PartnerGetCertificateResponseDto.class);

					if(partnerCertificateResponseObj!=null && partnerCertificateResponseObj.getResponse()!=null &&
							partnerCertificateResponseObj.getResponse().getCertificateData() !=null&& !partnerCertificateResponseObj.getResponse().getCertificateData().isEmpty()) {

						UploadCertificateRequestDto uploadCertificateRequestDto=new UploadCertificateRequestDto();
						uploadCertificateRequestDto.setApplicationId(applicationId);
						uploadCertificateRequestDto.setCertificateData(partnerCertificateResponseObj.getResponse().getCertificateData());
						uploadCertificateRequestDto.setReferenceId(partnerId);
						RequestWrapper<UploadCertificateRequestDto> uploadrequest=new RequestWrapper<UploadCertificateRequestDto>();
						uploadrequest.setRequest(uploadCertificateRequestDto);

						String uploadCertificateResponse=restUtil.postApi(ApiName.KEYMANAGER_UPLOAD_OTHER_DOMAIN_CERTIFICATE, null, "", "",
								MediaType.APPLICATION_JSON, uploadrequest, String.class);

						KeyManagerUploadCertificateResponseDto uploadCertificateResponseobj= mapper.readValue(uploadCertificateResponse,
								KeyManagerUploadCertificateResponseDto.class);

						if (uploadCertificateResponseobj != null && uploadCertificateResponseobj.getErrors() != null && !uploadCertificateResponseobj.getErrors().isEmpty()) {
							ServiceError error1 = uploadCertificateResponseobj.getErrors().get(0);
							throw new DataEncryptionFailureException(error1.getMessage());
						}

					}else if (partnerCertificateResponseObj != null && partnerCertificateResponseObj.getErrors() != null ) {
						ServiceError error2 = partnerCertificateResponseObj.getErrors();
						throw new DataEncryptionFailureException(error2.getMessage());
					}

				}
			}
			if(count==0) {
			ServiceError error = certificateResponseobj.getErrors().get(0);
			throw new DataEncryptionFailureException(error.getMessage());
			}
		}

	}

}
