package io.mosip.datashare.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import io.mosip.datashare.dto.DataSharePolicies;
import io.mosip.datashare.dto.Policies;
import io.mosip.datashare.dto.PolicyDetailResponseDto;
import io.mosip.datashare.dto.ShareableAttribute;

@Component
public class PolicyUtil {


	public PolicyDetailResponseDto getPolicyDetail(String policyId, String subscriberId) {
		// TODO call REST api of partner management
		// Now its Mocked to give PolicyDetailResponse
		PolicyDetailResponseDto policyDetailResponseDto = new PolicyDetailResponseDto();
		policyDetailResponseDto.setId("45678451034176");
		policyDetailResponseDto.setVersion("1.1");
		policyDetailResponseDto.setName("Digital QR Code Policy");
		policyDetailResponseDto.setDesc("");
		DataSharePolicies dataSharePolicies = new DataSharePolicies();
		dataSharePolicies.setEncryptionType("partnerBased");
		dataSharePolicies.setShareDomain("dev.mosip.net");
		dataSharePolicies.setTransactionsAllowed(2);
		dataSharePolicies.setValidForInMinutes(60);
		Policies policies = new Policies();
		policies.setDataSharePolicies(dataSharePolicies);
		List<ShareableAttribute> sharableAttributesList = new ArrayList<ShareableAttribute>();
		ShareableAttribute shareableAttribute1 = new ShareableAttribute();
		shareableAttribute1.setAttributeName("fullName");
		shareableAttribute1.setEncrypted(true);
		sharableAttributesList.add(shareableAttribute1);
		ShareableAttribute shareableAttribute2 = new ShareableAttribute();
		shareableAttribute2.setAttributeName("dateOfBirth");
		shareableAttribute2.setEncrypted(true);
		sharableAttributesList.add(shareableAttribute2);
		ShareableAttribute shareableAttribute3 = new ShareableAttribute();
		shareableAttribute3.setAttributeName("face");
		shareableAttribute3.setEncrypted(true);
		shareableAttribute3.setFormat("extraction");
		sharableAttributesList.add(shareableAttribute3);
		policies.setShareableAttributes(sharableAttributesList);
		policyDetailResponseDto.setPolicies(policies);
		return policyDetailResponseDto;

	}


}
