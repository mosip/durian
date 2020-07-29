package io.mosip.datashare.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.datashare.constant.LoggerFileConstant;
import io.mosip.datashare.dto.DataShare;
import io.mosip.datashare.dto.PolicyDetailResponse;
import io.mosip.datashare.exception.DataShareExpiredException;
import io.mosip.datashare.exception.DataShareNotFoundException;
import io.mosip.datashare.exception.FileException;
import io.mosip.datashare.exception.URLCreationException;
import io.mosip.datashare.logger.DataShareLogger;
import io.mosip.datashare.service.DataShareService;
import io.mosip.datashare.util.DigitalSignatureUtil;
import io.mosip.datashare.util.EncryptionUtil;
import io.mosip.datashare.util.PolicyUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;



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

	/** The account. */
	@Value("${mosip.datashare.account}")
	private String account;

	/** The container. */
	@Value("${mosip.datashare.container}")
	private String container;

	/** The Constant KEY_LENGTH. */
	private static final String KEY_LENGTH = "mosip.data.share.key.length";

	/** The Constant DEFAULT_KEY_LENGTH. */
	private static final int DEFAULT_KEY_LENGTH = 8;

	/** The Constant IO_EXCEPTION. */
	private static final String IO_EXCEPTION = "Exception while reading file";

	/** The Constant FORWARD_SLASH. */
	public static final String FORWARD_SLASH = "/";

	/** The Constant PROTOCOL. */
	public static final String PROTOCOL = "http";

	/** The Constant servletPath. */
	public static final String servletPath = "/v1/datashare/get";

	/** The Constant LOGGER. */
	private static final Logger LOGGER = DataShareLogger.getLogger(DataShareServiceImpl.class);
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.datashare.service.DataShareService#createDataShare(java.lang.String,
	 * java.lang.String, org.springframework.web.multipart.MultipartFile)
	 */
	@Override
	public DataShare createDataShare(String policyId, String subscriberId, MultipartFile file) {
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
				"DataShareServiceImpl::createDataShare()::entry");
		DataShare dataShare = new DataShare();
		if (file != null && !file.isEmpty()) {
			String randomShareKey;
			try {
				byte[] fileData = file.getBytes();

				PolicyDetailResponse policyDetailResponse = policyUtil.getPolicyDetail(policyId, subscriberId);

				Map<String, Object> aclMap = prepareMetaData(subscriberId, policyDetailResponse);

				if (policyDetailResponse.isEncryptionNeeded()) {

					byte[] encryptedData = encryptionUtil.encryptData(fileData, subscriberId);
					randomShareKey = storefile(aclMap, new ByteArrayInputStream(encryptedData));

				} else {

					randomShareKey = storefile(aclMap, new ByteArrayInputStream(fileData));

				}
				String dataShareUrl = constructURL(randomShareKey, policyDetailResponse.getShareDomain());

				dataShare.setSignature(digitalSignatureUtil.sign(fileData));
				dataShare.setUrl(dataShareUrl);
				dataShare.setPolicyId(policyId);
				dataShare.setSubscriberId(subscriberId);
				dataShare.setExtensionAllowed(policyDetailResponse.isExtensionAllowed());
				dataShare.setValidForInMinutes(policyDetailResponse.getValidForInMinutes());
				dataShare.setTransactionsAllowed(policyDetailResponse.getTransactionsAllowed());
				LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
						"Datashare" + dataShare.toString());
				LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), policyId,
						"DataShareServiceImpl::createDataShare()::exit");
			} catch (IOException e) {
				LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
						LoggerFileConstant.POLICYID.toString(),
						IO_EXCEPTION + ExceptionUtils.getStackTrace(e));
				throw new FileException(IO_EXCEPTION, e);
			}

		}else {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					LoggerFileConstant.POLICYID.toString(), DataUtilityErrorCodes.FILE_EXCEPTION.getErrorMessage());
			throw new FileException();
		}

		return dataShare;
	}

	/**
	 * Construct URL.
	 *
	 * @param randomShareKey the random share key
	 * @param shareDomain    the share domain
	 * @return the string
	 */
	private String constructURL(String randomShareKey, String shareDomain) {
		URL dataShareUrl = null;

		try {
			dataShareUrl = new URL(PROTOCOL, shareDomain, servletPath + FORWARD_SLASH + randomShareKey);
		} catch (MalformedURLException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(),
					LoggerFileConstant.POLICYID.toString(),
					DataUtilityErrorCodes.URL_CREATION_EXCEPTION.getErrorMessage() + ExceptionUtils.getStackTrace(e));
			new URLCreationException(e);
		}
		return dataShareUrl.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.datashare.service.DataShareService#getDataFile(java.lang.String)
	 */
	@Override
	public byte[] getDataFile(String randomShareKey) {
		byte[] dataBytes = null;
		try {
		boolean isDataShareAllow = getAndUpdateMetaData(randomShareKey);
		if (isDataShareAllow) {
			InputStream inputStream = objectStoreAdapter.getObject(account, container, randomShareKey);
			if (inputStream != null) {
				dataBytes = IOUtils.toByteArray(inputStream);
				} else {
					throw new DataShareNotFoundException();
			}
		} else {
				throw new DataShareExpiredException();
		}
		} catch (IOException e) {
			throw new FileException(IO_EXCEPTION, e);
		}
		return dataBytes;
	}

	/**
	 * Gets the and update meta data.
	 *
	 * @param randomShareKey the random share key
	 * @return the and update meta data
	 */
	private boolean getAndUpdateMetaData(String randomShareKey) {
		boolean isDataShareAllow=false;
		Map<String, Object> metaDataMap = objectStoreAdapter.getMetaData(account, container, randomShareKey);
		if (metaDataMap == null || metaDataMap.isEmpty()) {
			throw new DataShareNotFoundException();
		}else {
			int transactionAllowed=(int) metaDataMap.get("transactionsAllowed");
			if(transactionAllowed >= 1) {
				isDataShareAllow=true;
				metaDataMap.put("transactionsAllowed", transactionAllowed - 1);
			}

		}
		
		return isDataShareAllow;
	}


	/**
	 * Prepare meta data.
	 *
	 * @param subscriberId         the subscriber id
	 * @param policyDetailResponse the policy detail response
	 * @return the map
	 */
	private Map<String, Object> prepareMetaData(String subscriberId,
			PolicyDetailResponse policyDetailResponse) {
		// To do prepare ACL MAP as per policy details
		// Map created with mocked data
		Map<String, Object> aclMap = new HashMap<>();
		aclMap.put("subscriberId", subscriberId);
		aclMap.put("validForInMinutes", policyDetailResponse.getValidForInMinutes());
		aclMap.put("transactionsAllowed", policyDetailResponse.getTransactionsAllowed());
		aclMap.put("extensionallowed", policyDetailResponse.isExtensionAllowed());

		return aclMap;

	}


	/**
	 * Storefile.
	 *
	 * @param metaDataMap the meta data map
	 * @param filedata    the filedata
	 * @return the string
	 */
	private String storefile(Map<String, Object> metaDataMap, InputStream filedata) {
		int length = DEFAULT_KEY_LENGTH;
		if (env.getProperty(KEY_LENGTH) != null) {
			length = Integer.parseInt(env.getProperty(KEY_LENGTH));
		}

		String randomShareKey = RandomStringUtils.randomAlphanumeric(length);
		boolean isDataStored = objectStoreAdapter.putObject(account, container, randomShareKey, filedata);
		objectStoreAdapter.addObjectMetaData(account, container, randomShareKey, metaDataMap);
		LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.POLICYID.toString(), randomShareKey,
				"Is data stored to object store" + isDataStored);

		return randomShareKey;

	}
}
