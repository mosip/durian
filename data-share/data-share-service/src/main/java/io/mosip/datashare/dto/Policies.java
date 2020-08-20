package io.mosip.datashare.dto;

import java.util.List;

import lombok.Data;

@Data
public class Policies {

	private DataSharePolicies dataSharePolicies;

	private List<ShareableAttribute> shareableAttributes;
}
