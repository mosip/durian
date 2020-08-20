package io.mosip.datashare.exception;

import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class URLCreationException extends BaseUncheckedException {

	public URLCreationException() {
		super(DataUtilityErrorCodes.URL_CREATION_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.URL_CREATION_EXCEPTION.getErrorMessage());
    }

	public URLCreationException(String message) {
		super(DataUtilityErrorCodes.URL_CREATION_EXCEPTION.getErrorCode(),
                message);
    }

	public URLCreationException(Throwable e) {
		super(DataUtilityErrorCodes.URL_CREATION_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.URL_CREATION_EXCEPTION.getErrorMessage(), e);
    }

	public URLCreationException(String errorMessage, Throwable t) {
		super(DataUtilityErrorCodes.URL_CREATION_EXCEPTION.getErrorCode(), errorMessage, t);
    }
}
