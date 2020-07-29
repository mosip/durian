package io.mosip.datashare.dto;

import lombok.Data;

@Data
public class PolicyDetailResponse {

	private String policyName;
	
	/** The valid for in minutes. */
	private int validForInMinutes;
	
	/** The transactions allowed. */
	private int transactionsAllowed;
	
	/** The extension allowed. */
	private boolean extensionAllowed;
	
	private boolean isEncryptionNeeded;
	
	private String shareDomain;

}
