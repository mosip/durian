package io.mosip.datashare.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.mosip.datashare.dto.DataShare;
import io.mosip.datashare.dto.DataShareResponseDto;
import io.mosip.datashare.service.DataShareService;
import io.mosip.kernel.core.util.DateUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


/**
 * The Class DataShareController.
 */
@RestController
@Api(tags = "Data Share")
public class DataShareController {

	@Autowired
	private DataShareService dataShareService;

	@Autowired
	private Environment env;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.data.share.datetime.pattern";

	private static final String DATA_SHARE_SERVICE_ID = "mosip.data.share.service.id";

	private static final String DATA_SHARE_SERVICE_VERSION = "mosip.data.share.service.version";

	/**
	 * Creates the data share.
	 *
	 * @param file         the file
	 * @param policyId     the policy id
	 * @param subscriberId the subscriber id
	 * @return the response entity
	 */

	// @PreAuthorize("hasAnyRole('CREATE_SHARE')")
	@PostMapping(path = "/create/{policyId}/{subscriberId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get the share data url", response = DataShareResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Get Share Data URL successfully"),
			@ApiResponse(code = 400, message = "Unable to get share data url") })
	public ResponseEntity<Object> createDataShare(@RequestParam("file") MultipartFile file,
			@PathVariable("policyId") String policyId, @PathVariable("subscriberId") String subscriberId) {
		

		DataShare dataShare = dataShareService.createDataShare(policyId, subscriberId, file);
		return ResponseEntity.status(HttpStatus.OK)
				.body(buildDataShareResponse(dataShare));

	}

	private DataShareResponseDto buildDataShareResponse(DataShare dataShare) {
		DataShareResponseDto dataShareResponseDto = new DataShareResponseDto();
		dataShareResponseDto.setDataShare(dataShare);
		dataShareResponseDto.setId(DATA_SHARE_SERVICE_ID);
		dataShareResponseDto.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		dataShareResponseDto.setVersion(env.getProperty(DATA_SHARE_SERVICE_VERSION));
		return dataShareResponseDto;
	}

	/**
	 * Gets the file.
	 *
	 * @param randomShareKey the random share key
	 * @return the file
	 */

	@GetMapping(path = "/get/{policyId}/{subscriberId}/{randomShareKey}", consumes = MediaType.ALL_VALUE)
	@ApiOperation(value = "Get the data share file", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Get share data file successfully"),
			@ApiResponse(code = 400, message = "Unable to fetch file"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	@ResponseBody
	public ResponseEntity<byte[]> getFile(@PathVariable("policyId") String policyId,
			@PathVariable("subscriberId") String subscriberId, @PathVariable("randomShareKey") String randomShareKey) {
		// TODO need to validate JWT token with aud or azp which is client name or
		// subcriber id

		byte[] fileBytes = dataShareService.getDataFile(policyId, subscriberId, randomShareKey);
		return ResponseEntity.status(HttpStatus.OK).body(fileBytes);

	}
}
