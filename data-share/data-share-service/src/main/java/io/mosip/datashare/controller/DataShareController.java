package io.mosip.datashare.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import io.mosip.datashare.dto.DataShare;
import io.mosip.datashare.dto.DataShareGetResponse;
import io.mosip.datashare.dto.DataShareResponseDto;
import io.mosip.datashare.service.DataShareService;
import io.mosip.kernel.core.util.DateUtils2;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The Class DataShareController.
 */
@RestController
@Tag(name = "Data Share", description = "Data Share Controller")
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

	//@PreAuthorize("hasAnyRole('CREATE_SHARE')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostcreatepolicyidsubscriberid())")
	@PostMapping(path = "/create/{policyId}/{subscriberId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get the share data url", description = "Get the share data url", tags = { "Data Share" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Get Share Data URL successfully",
					content = @Content(schema = @Schema(implementation = DataShareResponseDto.class))),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "400", description = "Unable to get share data url" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> createDataShare(@RequestBody MultipartFile file,
			@PathVariable("policyId") String policyId, @PathVariable("subscriberId") String subscriberId,
			@Parameter(description = "Usage count for standalone mode") @RequestParam(required = false, name = "usageCountForStandaloneMode") String usageCountForStandaloneMode) {


		DataShare dataShare = dataShareService.createDataShare(policyId, subscriberId, file, usageCountForStandaloneMode);
		return ResponseEntity.status(HttpStatus.OK)
				.body(buildDataShareResponse(dataShare));

	}

	private DataShareResponseDto buildDataShareResponse(DataShare dataShare) {
		DataShareResponseDto dataShareResponseDto = new DataShareResponseDto();
		dataShareResponseDto.setDataShare(dataShare);
		dataShareResponseDto.setId(DATA_SHARE_SERVICE_ID);
		dataShareResponseDto.setResponsetime(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
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
	@Operation(summary = "Get the data share file", description = "Get the data share file", tags = { "Data Share" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Get share data file successfully",
					content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "400", description = "Unable to fetch file" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error" ,content = @Content(schema = @Schema(hidden = true)))
	})
	@ResponseBody
	public ResponseEntity<byte[]> getFile(@PathVariable("policyId") String policyId,
			@PathVariable("subscriberId") String subscriberId, @PathVariable("randomShareKey") String randomShareKey) {

		DataShareGetResponse dataShareGetResponse = dataShareService.getDataFile(policyId, subscriberId,
				randomShareKey);
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		headers.add("Signature", dataShareGetResponse.getSignature());

		return new ResponseEntity<byte[]>(dataShareGetResponse.getFileBytes(), headers, HttpStatus.OK);

	}

	/**
	 * Gets the file.
	 *
	 * @param randomShareKey the random share key
	 * @return the file
	 */

	@GetMapping(path = "/datashare/{shortUrlKey}", consumes = MediaType.ALL_VALUE)
	@Operation(summary = "Get the data share file", description = "Get the data share file", tags = { "Data Share" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Get share data file successfully",
					content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "400", description = "Unable to fetch file" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error" ,content = @Content(schema = @Schema(hidden = true)))})
	@ResponseBody
	public ResponseEntity<byte[]> getFile(@PathVariable("shortUrlKey") String shortUrlKey) {


		DataShareGetResponse dataShareGetResponse = dataShareService.getDataFile(shortUrlKey);
		  MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		  headers.add("Signature", dataShareGetResponse.getSignature());

		return new ResponseEntity<byte[]>(dataShareGetResponse.getFileBytes(), headers,
				HttpStatus.OK);

	}
}
