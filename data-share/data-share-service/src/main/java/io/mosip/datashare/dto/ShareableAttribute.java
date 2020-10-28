package io.mosip.datashare.dto;

import lombok.Data;

@Data
public class ShareableAttribute {

	private String attributeName;
	
	private boolean encrypted;
	
	private String format;
}
