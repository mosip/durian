package io.mosip.datashare.dto;

import lombok.Data;

@Data
public class DataShareDto {
	
	private String validForInMinutes;
	
	private String transactionsAllowed;
	
	private String encryptionType;
	
	private String shareDomain;

	private String shareDomainUrlWrite;

	private String shareDomainUrlRead;
	
	private String typeOfShare;
	
	private String source;
}
