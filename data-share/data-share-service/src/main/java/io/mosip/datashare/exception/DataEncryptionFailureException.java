package io.mosip.datashare.exception;

import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;


public class DataEncryptionFailureException extends BaseUncheckedException {

	/** Serializable version Id. */
	private static final long serialVersionUID = 1L;

	public DataEncryptionFailureException() {
		super(DataUtilityErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorMessage());
	}

	public DataEncryptionFailureException(Throwable t) {
		super(DataUtilityErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorMessage(), t);
	}

	/**
	 * @param message
	 *            Message providing the specific context of the error.
	 * @param cause
	 *            Throwable cause for the specific exception
	 */
	public DataEncryptionFailureException(String message, Throwable cause) {
		super(DataUtilityErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(), message, cause);

	}

	public DataEncryptionFailureException(String errorMessage) {
		super(DataUtilityErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(), errorMessage);
	}

}