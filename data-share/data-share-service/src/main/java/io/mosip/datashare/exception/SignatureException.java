package io.mosip.datashare.exception;

import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class SignatureException extends BaseUncheckedException {
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new file not found in destination exception.
	 */
	public SignatureException() {
		super();

	}


	public SignatureException(String errorMessage) {
		super(DataUtilityErrorCodes.SIGNATURE_EXCEPTION.getErrorCode(), errorMessage);
	}


	public SignatureException(String message, Throwable cause) {
		super(DataUtilityErrorCodes.SIGNATURE_EXCEPTION.getErrorCode() + EMPTY_SPACE, message, cause);

	}

	public SignatureException(Throwable t) {
		super(DataUtilityErrorCodes.SIGNATURE_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.SIGNATURE_EXCEPTION.getErrorMessage(), t);
	}
}
