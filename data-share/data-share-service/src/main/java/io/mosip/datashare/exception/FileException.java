package io.mosip.datashare.exception;

import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class FileException extends BaseUncheckedException {

	/** Serializable version Id. */
	private static final long serialVersionUID = 1L;

	public FileException() {
		super(DataUtilityErrorCodes.FILE_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.FILE_EXCEPTION.getErrorMessage());
	}

	public FileException(Throwable t) {
		super(DataUtilityErrorCodes.FILE_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.FILE_EXCEPTION.getErrorMessage(), t);
	}

	/**
	 * @param message
	 *            Message providing the specific context of the error.
	 * @param cause
	 *            Throwable cause for the specific exception
	 */
	public FileException(String message, Throwable cause) {
		super(DataUtilityErrorCodes.FILE_EXCEPTION.getErrorCode(), message, cause);

	}

	public FileException(String errorMessage) {
		super(DataUtilityErrorCodes.FILE_EXCEPTION.getErrorCode(), errorMessage);
	}
}
