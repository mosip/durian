package io.mosip.datashare.exception;

import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class DataShareExpiredException extends BaseUncheckedException {

	private static final long serialVersionUID = 1L;

	public DataShareExpiredException() {
		super(DataUtilityErrorCodes.DATA_SHARE_EXPIRED_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.DATA_SHARE_EXPIRED_EXCEPTION.getErrorMessage());
	}

	public DataShareExpiredException(Throwable t) {
		super(DataUtilityErrorCodes.DATA_SHARE_EXPIRED_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.DATA_SHARE_EXPIRED_EXCEPTION.getErrorMessage(), t);
	}

	/**
	 * @param message
	 *            Message providing the specific context of the error.
	 * @param cause
	 *            Throwable cause for the specific exception
	 */
	public DataShareExpiredException(String message, Throwable cause) {
		super(DataUtilityErrorCodes.DATA_SHARE_EXPIRED_EXCEPTION.getErrorCode(), message, cause);

	}

	public DataShareExpiredException(String errorMessage) {
		super(DataUtilityErrorCodes.DATA_SHARE_EXPIRED_EXCEPTION.getErrorCode(), errorMessage);
	}

}
