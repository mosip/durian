package io.mosip.datashare.exception;

import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class ApiNotAccessibleException extends BaseUncheckedException {

    public ApiNotAccessibleException() {
		super(DataUtilityErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
    }

    public ApiNotAccessibleException(String message) {
		super(DataUtilityErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
                message);
    }

    public ApiNotAccessibleException(Throwable e) {
		super(DataUtilityErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				DataUtilityErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage(), e);
    }

    public ApiNotAccessibleException(String errorMessage, Throwable t) {
		super(DataUtilityErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(), errorMessage, t);
    }


}
