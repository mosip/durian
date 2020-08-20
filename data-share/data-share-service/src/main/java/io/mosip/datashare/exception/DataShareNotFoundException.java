package io.mosip.datashare.exception;

import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class DataShareNotFoundException extends BaseUncheckedException {

	private static final long serialVersionUID = 1L;

	public DataShareNotFoundException() {
		super(DataUtilityErrorCodes.DATA_SHARE_NOT_FOUND_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.DATA_SHARE_NOT_FOUND_EXCEPTION.getErrorMessage());
	}

	public DataShareNotFoundException(Throwable t) {
		super(DataUtilityErrorCodes.DATA_SHARE_NOT_FOUND_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.DATA_SHARE_NOT_FOUND_EXCEPTION.getErrorMessage(), t);
	}

	/**
	 * @param message
	 *            Message providing the specific context of the error.
	 * @param cause
	 *            Throwable cause for the specific exception
	 */
	public DataShareNotFoundException(String message, Throwable cause) {
		super(DataUtilityErrorCodes.DATA_SHARE_NOT_FOUND_EXCEPTION.getErrorCode(), message, cause);

	}

	public DataShareNotFoundException(String errorMessage) {
		super(DataUtilityErrorCodes.DATA_SHARE_NOT_FOUND_EXCEPTION.getErrorCode(), errorMessage);
	}

}
