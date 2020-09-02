package io.mosip.datashare.exception;

import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class PolicyException extends BaseUncheckedException {
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new file not found in destination exception.
	 */
	public PolicyException() {
		super();

	}

	public PolicyException(String errorMessage) {
		super(DataUtilityErrorCodes.POLICY_EXCEPTION.getErrorCode(), errorMessage);
	}

	public PolicyException(String message, Throwable cause) {
		super(DataUtilityErrorCodes.POLICY_EXCEPTION.getErrorCode() + EMPTY_SPACE, message, cause);

	}

	public PolicyException(Throwable t) {
		super(DataUtilityErrorCodes.POLICY_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.POLICY_EXCEPTION.getErrorMessage(), t);
	}
}
