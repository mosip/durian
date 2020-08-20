package io.mosip.datashare.dto;

import java.util.List;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyResponse extends BaseRestResponseDTO {
	private static final long serialVersionUID = 1L;

	private PolicyDetailResponseDto response;

	private List<ErrorDTO> errors;
}
