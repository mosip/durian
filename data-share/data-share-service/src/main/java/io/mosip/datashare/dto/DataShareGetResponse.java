package io.mosip.datashare.dto;

import lombok.Data;

@Data
public class DataShareGetResponse {

	private byte[] fileBytes;
	
	private String signature;
}
