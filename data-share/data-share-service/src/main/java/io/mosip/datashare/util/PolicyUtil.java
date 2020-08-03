package io.mosip.datashare.util;

import org.springframework.stereotype.Component;

import io.mosip.datashare.dto.PolicyDetailResponse;

@Component
public class PolicyUtil {


	public PolicyDetailResponse getPolicyDetail(String policyId, String subscriberId) {
		// TODO call REST api of partner management
		// Now its Mocked to give PolicyDetailResponse

		PolicyDetailResponse policyDetailResponse = new PolicyDetailResponse();
		policyDetailResponse.setEncryptionNeeded(true);
		policyDetailResponse.setExtensionAllowed(true);
		policyDetailResponse.setValidForInMinutes(60);
		policyDetailResponse.setTransactionsAllowed(2);
		policyDetailResponse.setShareDomain("mosip.ip");
		policyDetailResponse.setSha256("");
		return policyDetailResponse;
	                	 
	}


}
