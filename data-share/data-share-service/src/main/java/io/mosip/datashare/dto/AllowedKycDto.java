package io.mosip.datashare.dto;

import lombok.Data;

@Data
public class AllowedKycDto {

	public String attributeName;	

	public boolean encrypted;
	
	public String format;
}
