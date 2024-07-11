package io.mosip.datashare.exception;

import io.mosip.datashare.constant.DataUtilityErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class StaticDataShareException extends BaseUncheckedException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    public StaticDataShareException() {
        super();

    }

    public StaticDataShareException(String errorMessage) {
        super(DataUtilityErrorCodes.STATIC_DATA_SHARE_EXCEPTION.getErrorCode(), errorMessage);
    }

    public StaticDataShareException(String message, Throwable cause) {
        super(DataUtilityErrorCodes.STATIC_DATA_SHARE_EXCEPTION.getErrorCode() + EMPTY_SPACE, message, cause);

    }

    public StaticDataShareException(Throwable t) {
        super(DataUtilityErrorCodes.STATIC_DATA_SHARE_EXCEPTION.getErrorCode(),
                DataUtilityErrorCodes.STATIC_DATA_SHARE_EXCEPTION.getErrorMessage(), t);
    }
}
