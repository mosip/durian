package io.mosip.datashare.constant;

public enum DataUtilityErrorCodes {


	DATA_ENCRYPTION_FAILURE_EXCEPTION("DAT-SER-001", "Data Encryption failed"),
	API_NOT_ACCESSIBLE_EXCEPTION("DAT-SER-002", "API not accessible"),
	FILE_EXCEPTION("DAT-SER-003", "File is not exists"),
	URL_CREATION_EXCEPTION("DAT-SER-004", "URL creation exception"),
	SIGNATURE_EXCEPTION("DAT-SER-005", "Failed to generate digital signature"),
	DATA_SHARE_NOT_FOUND_EXCEPTION("DAT-SER-006", "Data share not found"),
	DATA_SHARE_EXPIRED_EXCEPTION("DAT-SER-006", "Data share usuage expired"),
	POLICY_EXCEPTION("DAT-SER-007", "Exception while fetching policy details");

	private final String errorCode;
	private final String errorMessage;

	private DataUtilityErrorCodes(final String errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
