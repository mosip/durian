package io.mosip.datashare.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.mosip.datashare.dto.DataShare;
import io.mosip.datashare.dto.DataShareGetResponse;


/**
 * The Interface DataShareService.
 */
@Service
public interface DataShareService {

	/**
	 * Creates the data share.
	 *
	 * @param policyId     the policy id
	 * @param subscriberId the subscriber id
	 * @param file         the file
	 * @return the data share
	 */
	public DataShare createDataShare(String policyId, String subscriberId, MultipartFile file);

	/**
	 * Gets the data file.
	 *
	 * @param randomShareKey the random share key
	 * @return the data file
	 */
	public DataShareGetResponse getDataFile(String policyId, String subscriberId, String randomShareKey);

	public DataShareGetResponse getDataFile(String randomShareKey);




}
