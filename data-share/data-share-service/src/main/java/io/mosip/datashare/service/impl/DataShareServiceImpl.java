package io.mosip.datashare.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.datashare.constant.LoggerFileConstant;
import io.mosip.datashare.dto.DataShare;
import io.mosip.datashare.dto.DataShareDto;
import io.mosip.datashare.dto.DataShareGetResponse;
import io.mosip.datashare.dto.PolicyResponseDto;
import io.mosip.datashare.exception.DataShareExpiredException;
import io.mosip.datashare.exception.DataShareNotFoundException;
import io.mosip.datashare.exception.FileException;
import io.mosip.datashare.logger.DataShareLogger;
import io.mosip.datashare.service.DataShareService;
import io.mosip.datashare.util.CacheUtil;
import io.mosip.datashare.util.DigitalSignatureUtil;
import io.mosip.datashare.util.EncryptionUtil;
import io.mosip.datashare.util.PolicyUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils2;



// TODO: Auto-generated Javadoc
/**
 * The Class DataShareServiceImpl.
 */
@RefreshScope
@Component
public class DataShareServiceImpl implements DataShareService {

	/** The policy util. */
	@Autowired
	private PolicyUtil policyUtil;

	/** The encryption util. */
	@Autowired
	private EncryptionUtil encryptionUtil;

	/** The env. */
	@Autowired
	private Environment env;

	/** The digital signature util. */
	@Autowired
	private DigitalSignatureUtil digitalSignatureUtil;

	/** The object store adapter. */
	@Autowired
	ObjectStoreAdapter objectStoreAdapter;

	/** The cache util. */
	@Autowired
	private CacheUtil cacheUtil;

	/** Shared executor for parallelising independent per-request operations. */
	@Autowired
	@Qualifier("dataShareTaskExecutor")
	private TaskExecutor taskExecutor;

	/** The Constant KEY_LENGTH. */
	private static final String KEY_LENGTH = "mosip.data.share.key.length";

	/** The Constant DEFAULT_KEY_LENGTH. */
	private static final int DEFAULT_KEY_LENGTH = 8;

	/**
	 * Singleton SecureRandom — thread-safe; avoids per-request OS seeding overhead.
	 */
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	/** Key length for random share keys, cached from config at startup. */
	@Value("${" + KEY_LENGTH + ":8}")
	private int keyLength;

	/** The Constant IO_EXCEPTION. */
	private static final String IO_EXCEPTION = "Exception while reading file";

	/** The Constant FORWARD_SLASH. */
	public static final String FORWARD_SLASH = "/";

	/** The Constant PROTOCOL. */
	public static final String HTTPS_PROTOCOL = "https://";

	/** The Constant PROTOCOL. */
	public static final String HTTP_PROTOCOL = "http://";

	/** The Constant servletPath. */
	public static final String GET = "get";

	public static final String DATASHARE = "datashare";

	/** The Constant LOGGER. */
	private static final Logger LOGGER = DataShareLogger.getLogger(DataShareServiceImpl.class);

	/** The servlet path. */
	@Value("${server.servlet.path}")
	private String servletPath;

	/** The is short url. */
	@Value("${mosip.data.share.urlshortner}")
	private boolean isShortUrl;

	public static final String PARTNERBASED = "Partner Based";

	public static final String NONE = "none";

	public static final String TRANSACTIONSALLOWED = "transactionsallowed";

	public static final String SIGNATURE = "signature";

	@Value("${mosip.data.share.protocol}")
	private String httpProtocol;

	/** Defines whether static data share policy needs to be used for sharing the data*/
	@Value("${mosip.data.share.standalone.mode.enabled:false}")
	private boolean standaloneModeEnabled;

	/** Defines whether JWT signature generation needs to be disabled */
	@Value("${mosip.data.share.signature.disabled:false}")
	private boolean isSignatureDisabled;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.data.share.datetime.pattern";

	/** The constant defines unlimited usage count for the created share */
	public static final int UNLIMITED_USAGE_COUNT = -1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.datashare.service.DataShareService#createDataShare(java.lang.String,
	 * java.lang.String, org.springframework.web.multipart.MultipartFile)
	 */
	@Override
	public DataShare createDataShare(String policyId, String subscriberId, MultipartFile file,
									 String usageCountForStandaloneMode) {
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
				"DataShareServiceImpl::createDataShare()::entry");
		DataShare dataShare = new DataShare();
		if (file != null && !file.isEmpty()) {
			String randomShareKey;
			try {
				byte[] fileData = file.getBytes();
				DataShareDto dataSharePolicy;
				LocalDateTime policyPublishDate = null;
				LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
						"standaloneModeEnabled : " + standaloneModeEnabled + ", isSignatureDisabled : " + isSignatureDisabled);
				if(!standaloneModeEnabled) {
					PolicyResponseDto policyDetailResponse = policyUtil.getPolicyDetail(policyId, subscriberId);
					dataSharePolicy = policyDetailResponse.getPolicies().getDataSharePolicies();
					policyPublishDate = policyDetailResponse.getPublishDate();
				} else {
					dataSharePolicy = policyUtil.getStaticDataSharePolicy(policyId, subscriberId, usageCountForStandaloneMode);
				}
				// Compute timestamps first (pure CPU, no I/O)
				String createShareTime = DateUtils2
						.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN));
				String expiryTime = DateUtils2
						.toISOString(DateUtils2.addMinutes(DateUtils2.parseUTCToDate(createShareTime),
								Integer.parseInt(dataSharePolicy.getValidForInMinutes())));

				// Launch encryption and JWT signing in parallel — they are independent of each other
				final String encType = dataSharePolicy.getEncryptionType();
				final byte[] fd = fileData;
				final String sid = subscriberId;

				CompletableFuture<byte[]> encryptFuture;
				if (PARTNERBASED.equalsIgnoreCase(encType)) {
					LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
							policyId, subscriberId + "encryptionNeeded" + encType);
					encryptFuture = CompletableFuture.supplyAsync(
							() -> encryptionUtil.encryptData(fd, sid), taskExecutor);
				} else {
					LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
							policyId, subscriberId + "Without encryption" + encType);
					encryptFuture = CompletableFuture.completedFuture(fd);
				}

				final String cst = createShareTime;
				final String et = expiryTime;
				final String fn = file.getName();
				CompletableFuture<String> signFuture = isSignatureDisabled
						? CompletableFuture.completedFuture("")
						: CompletableFuture.supplyAsync(
								() -> digitalSignatureUtil.jwtSign(fd, fn, sid, cst, et), taskExecutor);

				byte[] encryptedData;
				String jwtSignature;
				try {
					CompletableFuture.allOf(encryptFuture, signFuture).join();
					encryptedData = encryptFuture.getNow(fd);
					jwtSignature = signFuture.getNow("");
				} catch (CompletionException ce) {
					Throwable cause = ce.getCause();
					if (cause instanceof RuntimeException) {
						throw (RuntimeException) cause;
					}
					throw new FileException("Async operation failed", new IOException(cause));
				}
				Map<String, Object> aclMap = prepareMetaData(subscriberId, policyId, dataSharePolicy,
						jwtSignature, policyPublishDate);
				randomShareKey = storefile(aclMap, new ByteArrayInputStream(encryptedData), policyId, subscriberId);
				String dataShareUrl = constructURL(randomShareKey, dataSharePolicy, policyId,
						subscriberId);


				dataShare.setUrl(dataShareUrl);
				dataShare.setPolicyId(policyId);
				dataShare.setSubscriberId(subscriberId);
				dataShare.setValidForInMinutes(Integer.parseInt(dataSharePolicy.getValidForInMinutes()));
				dataShare.setTransactionsAllowed(Integer.parseInt(dataSharePolicy.getTransactionsAllowed()));
				LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
						"Datashare" + dataShare.toString());
				LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
						"DataShareServiceImpl::createDataShare()::exit");
			} catch (IOException e) {
				LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
						policyId, IO_EXCEPTION + ExceptionUtils.getStackTrace(e));
				throw new FileException(IO_EXCEPTION, e);
			}

		}else {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					policyId, DataUtilityErrorCodes.FILE_EXCEPTION.getErrorMessage());
			throw new FileException();
		}

		return dataShare;
	}

	/**
	 * Construct URL.
	 *
	 * @param randomShareKey the random share key
	 * @param shareDomain    the share domain
	 * @param policyId       the policy id
	 * @param subscriberId   the subscriber id
	 * @return the string
	 */
	private String constructURL(String randomShareKey, DataShareDto dataSharePolicy, String policyId, String subscriberId) {
		String protocol = (dataSharePolicy.getProtocol() != null) ? dataSharePolicy.getProtocol() :HTTP_PROTOCOL ;
		String url = null;
		if (isShortUrl) {
			String shortRandomShareKey = generateShortRandomShareKey(keyLength);
			cacheUtil.getShortUrlData(shortRandomShareKey, policyId, subscriberId, randomShareKey);
			url = dataSharePolicy.getShareDomainUrlRead() != null ?
					dataSharePolicy.getShareDomainUrlRead() +
							servletPath + DATASHARE + FORWARD_SLASH + shortRandomShareKey
					:
					protocol + dataSharePolicy.getShareDomain() +
					servletPath + DATASHARE + FORWARD_SLASH + shortRandomShareKey;

		} else {
			url = dataSharePolicy.getShareDomainUrlRead() != null ?
					dataSharePolicy.getShareDomainUrlRead() +
							servletPath + FORWARD_SLASH + GET + FORWARD_SLASH
							+ policyId + FORWARD_SLASH + subscriberId + FORWARD_SLASH + randomShareKey
					: protocol + dataSharePolicy.getShareDomain() + servletPath + FORWARD_SLASH + GET + FORWARD_SLASH
					+ policyId + FORWARD_SLASH + subscriberId + FORWARD_SLASH + randomShareKey;
		}
		url = url.replaceAll("[\\[\\]]", "");

		return url;
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.datashare.service.DataShareService#getDataFile(java.lang.String)
	 */
	@Override
	public DataShareGetResponse getDataFile(String policyId, String subcriberId, String randomShareKey) {
		DataShareGetResponse dataShareGetResponse = new DataShareGetResponse();
		byte[] dataBytes = null;
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
				"DataShareServiceImpl::getDataFile()::entry");
		try {
			boolean isDataShareAllow = getAndUpdateMetaData(randomShareKey, policyId, subcriberId,
					dataShareGetResponse);
			if (isDataShareAllow) {
				InputStream inputStream = objectStoreAdapter.getObject(subcriberId, policyId, null, null,
						randomShareKey);
				if (inputStream != null) {
					dataBytes = IOUtils.toByteArray(inputStream);
					dataShareGetResponse.setFileBytes(dataBytes);
					LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
							policyId, "Successfully get the object from object store");
				} else {
					LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
							policyId, "Failed to get object from object store");
					throw new DataShareNotFoundException();
				}
			} else {
				throw new DataShareExpiredException();
			}
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
					"DataShareServiceImpl::getDataFile()::exit");
		}
		catch (ObjectStoreAdapterException e){
			throw new DataShareNotFoundException();
		}
		catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
					IO_EXCEPTION + ExceptionUtils.getStackTrace(e));
			throw new FileException(IO_EXCEPTION, e);
		}

		return dataShareGetResponse;
	}

	/**
	 * Gets the and update meta data.
	 *
	 * @param randomShareKey the random share key
	 * @param policyId       the policy id
	 * @param subcriberId    the subcriber id
	 * @return the and update meta data
	 */
	private boolean getAndUpdateMetaData(String randomShareKey, String policyId, String subcriberId,
			DataShareGetResponse dataShareGetResponse) {
		boolean isDataShareAllow = false;
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
				"DataShareServiceImpl::getAndUpdateMetaData()::entry");
		Map<String, Object> metaDataMap = objectStoreAdapter.getMetaData(subcriberId, policyId, null, null,
				randomShareKey);
		if (metaDataMap == null || metaDataMap.isEmpty()) {
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
					"metadata is empty");
			throw new DataShareNotFoundException();
		}else {
			dataShareGetResponse.setSignature((String) metaDataMap.get(SIGNATURE));
			int transactionAllowed = Integer.parseInt((String) metaDataMap.get(TRANSACTIONSALLOWED));
			if(transactionAllowed >= 1) {
				isDataShareAllow=true;
				metaDataMap.put(TRANSACTIONSALLOWED, transactionAllowed- 1);
				objectStoreAdapter.addObjectMetaData(subcriberId, policyId, null, null, randomShareKey, metaDataMap);
				LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
						"Successfully update the metadata");
			}
			/* Unlimited usage is allowed hence not updating the metadata*/
			if(transactionAllowed == UNLIMITED_USAGE_COUNT) {
				isDataShareAllow = true;
				LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
						"Unlimited usage of data share is configured hence not updating metadata");
			}

		}
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
				"DataShareServiceImpl::getAndUpdateMetaData()::exit");
		return isDataShareAllow;
	}


	/**
	 * Prepare meta data.
	 *
	 * @param subscriberId         the subscriber id
	 * @param policyId             the policy id
	 * @param dataSharePolicies    the data share policies
	 * @param jwtSignature 		   the jwt signature for shared object
	 * @param policyPublishDate 		   the policy publish date
	 * @return the map
	 */
	private Map<String, Object> prepareMetaData(String subscriberId, String policyId,
												DataShareDto dataSharePolicies, String jwtSignature, LocalDateTime policyPublishDate) {

		Map<String, Object> aclMap = new HashMap<>();

		aclMap.put("policyid", policyId);
		aclMap.put("policypublishdate", policyPublishDate);
		aclMap.put("subscriberId", subscriberId);
		aclMap.put("validforinminutes", dataSharePolicies.getValidForInMinutes());
		aclMap.put("transactionsallowed", dataSharePolicies.getTransactionsAllowed());
		aclMap.put("signature", jwtSignature);


		return aclMap;

	}


	/**
	 * Storefile.
	 *
	 * @param metaDataMap  the meta data map
	 * @param filedata     the filedata
	 * @param policyId     the policy id
	 * @param subscriberId the subscriber id
	 * @return the string
	 */
	private String storefile(Map<String, Object> metaDataMap, InputStream filedata, String policyId,
			String subscriberId) {
		String randomShareKey = subscriberId + policyId
				+ DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
				+ generateShortRandomShareKey(keyLength);
		boolean isDataStored = objectStoreAdapter.putObject(subscriberId, policyId, null, null, randomShareKey,
				filedata);
		objectStoreAdapter.addObjectMetaData(subscriberId, policyId, null, null, randomShareKey, metaDataMap);
		LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), randomShareKey,
				"Is data stored to object store" + isDataStored);

		return randomShareKey;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.datashare.service.DataShareService#getDataFile(java.lang.String)
	 */
	@Override
	public DataShareGetResponse getDataFile(String shortUrlKey) {
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.KEY.toString(), shortUrlKey,
				"DataShareServiceImpl::getDataFile()");
		String data = cacheUtil.getShortUrlData(shortUrlKey, null, null, null);
		
		if (data != null && !data.isEmpty()) {
			String[] datas = data.split(",");
			if (datas != null && datas.length == 3) {
				return getDataFile(datas[0], datas[1], datas[2]);
			} else {
				throw new DataShareNotFoundException();
			}

		} else {
			throw new DataShareNotFoundException();
		}


	}

	private static String generateShortRandomShareKey(int byteLength) {
		byte[] token = new byte[byteLength];
		SECURE_RANDOM.nextBytes(token);
		return CryptoUtil.encodeToURLSafeBase64(token);
	}

}