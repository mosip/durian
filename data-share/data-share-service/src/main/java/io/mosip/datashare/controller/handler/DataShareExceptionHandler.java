package io.mosip.datashare.controller.handler;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.datashare.controller.DataShareController;
import io.mosip.datashare.dto.DataShareResponseDto;
import io.mosip.datashare.dto.ErrorDTO;
import io.mosip.datashare.exception.ApiNotAccessibleException;
import io.mosip.datashare.exception.DataEncryptionFailureException;
import io.mosip.datashare.exception.DataShareExpiredException;
import io.mosip.datashare.exception.DataShareNotFoundException;
import io.mosip.datashare.exception.FileException;
import io.mosip.datashare.exception.PolicyException;
import io.mosip.datashare.exception.SignatureException;
import io.mosip.datashare.exception.URLCreationException;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.util.DateUtils;

@RestControllerAdvice(assignableTypes = DataShareController.class)
public class DataShareExceptionHandler {

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.data.share.datetime.pattern";
	
	private static final String DATA_SHARE_SERVICE_ID = "mosip.data.share.service.id";
	
	private static final String DATA_SHARE_SERVICE_VERSION = "mosip.data.share.service.version";

	/** The env. */
	@Autowired
	private Environment env;

	@ExceptionHandler(DataEncryptionFailureException.class)
	public ResponseEntity<DataShareResponseDto> dataEncryptionException(DataEncryptionFailureException e) {
		return buildDataShareApiExceptionResponse((Exception) e);
	}
	
	@ExceptionHandler(ApiNotAccessibleException.class)
	public ResponseEntity<DataShareResponseDto> apiNotAccessibleException(ApiNotAccessibleException e) {
		return buildDataShareApiExceptionResponse((Exception) e);
	}
	
	@ExceptionHandler(FileException.class)
	public ResponseEntity<DataShareResponseDto> fileException(FileException e) {
		return buildDataShareApiExceptionResponse((Exception) e);
	}

	@ExceptionHandler(SignatureException.class)
	public ResponseEntity<DataShareResponseDto> signatureException(SignatureException e) {
		return buildDataShareApiExceptionResponse((Exception) e);
	}

	@ExceptionHandler(DataShareExpiredException.class)
	public ResponseEntity<DataShareResponseDto> dataShareExpiredException(DataShareExpiredException e) {
		return buildDataShareApiExceptionResponse((Exception) e);
	}

	@ExceptionHandler(DataShareNotFoundException.class)
	public ResponseEntity<DataShareResponseDto> dataShareNotFoundException(DataShareNotFoundException e) {
		return buildDataShareApiExceptionResponse((Exception) e);
	}

	@ExceptionHandler(URLCreationException.class)
	public ResponseEntity<DataShareResponseDto> uRLCreationException(URLCreationException e) {
		return buildDataShareApiExceptionResponse((Exception) e);
	}

	@ExceptionHandler(PolicyException.class)
	public ResponseEntity<DataShareResponseDto> policyException(PolicyException e) {
		return buildDataShareApiExceptionResponse((Exception) e);
	}

	@ExceptionHandler(ObjectStoreAdapterException.class)
	public ResponseEntity<DataShareResponseDto> objectStoreAdapterException(ObjectStoreAdapterException e) {
		return buildDataShareApiExceptionResponse((Exception) e);
	}
	private ResponseEntity<DataShareResponseDto> buildDataShareApiExceptionResponse(Exception ex) {
		DataShareResponseDto response = new DataShareResponseDto();
		Throwable e = ex;

		if (Objects.isNull(response.getId())) {
			response.setId(env.getProperty(DATA_SHARE_SERVICE_ID));
		}
		if (e instanceof BaseCheckedException) {
			List<String> errorCodes = ((BaseCheckedException) e).getCodes();
			List<String> errorTexts = ((BaseCheckedException) e).getErrorTexts();

			List<ErrorDTO> errors = errorTexts.parallelStream()
					.map(errMsg -> new ErrorDTO(errorCodes.get(errorTexts.indexOf(errMsg)), errMsg)).distinct()
					.collect(Collectors.toList());
			response.setErrors(errors);
		}
		if (e instanceof BaseUncheckedException) {
			List<String> errorCodes = ((BaseUncheckedException) e).getCodes();
			List<String> errorTexts = ((BaseUncheckedException) e).getErrorTexts();

			List<ErrorDTO> errors = errorTexts.parallelStream()
					.map(errMsg -> new ErrorDTO(errorCodes.get(errorTexts.indexOf(errMsg)), errMsg)).distinct()
					.collect(Collectors.toList());
			response.setErrors(errors);
		}
		response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(DATA_SHARE_SERVICE_VERSION));

		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response);
	}


}
