# Durian 
[![Maven Package upon a push](https://github.com/mosip/durian/actions/workflows/push_trigger.yml/badge.svg?branch=release-1.2.0.1)](https://github.com/mosip/durian/actions/workflows/push_trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.2.0.1&project=mosip_durian&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.2.0.1&id=mosip_durian)


## Overview
Refer to [MOSIP docs](https://docs.mosip.io/1.2.0/modules/datashare).

## APIs
API documentation is available [here](https://docs.mosip.io/1.2.0/api).

## Durian in standalone mode
Durian application has dependency on the external services such as partner management service for policy verification and key management service for crypto operations such as signature computation, Encryption etc.
Durian application can be configured in the standalone mode where it doesn't depend on the external services to perform the operations.
To configure the same, below properties should be used:
1. **mosip.data.share.standalone.mode.enabled:** This property enables application to run in standalone mode. The value for the property should be **true**.
2. **mosip.data.share.static-policy.policy-json:** This property contains policy JSON which will used as static policy for the data share creation in standalone mode. The property value can be below:
   {"typeOfShare":"","transactionsAllowed":"2","shareDomain":"datashare.datashare","encryptionType":"NONE","source":"","validForInMinutes":"30"}  
   **transactionsAllowed** attribute value of -1 allows unlimited transaction on the created share.  
   **encryptionType** attribute is kept as **NONE** so that encryption won't be performed and dependency on key manager will not be there.
3. **mosip.data.share.static-policy.policy-id:** This property contains the policy id which will be used for creating the data share. This property must match with the {policyId} received in the **/create** API otherwise error will be thrown. 
4. **mosip.data.share.static-policy.subscriber-id:** This property contains the subscriber id which will be used for creating the data share. This property must match with the {subscriberId} received in the **/create** API otherwise error will be thrown.
5. **mosip.data.share.signature.disabled:** This property enables/disables the signature computation for the created data share. This property value must be **true**.

Standalone mode enablement is not advisable because it bypasses the policy verification and signature computation for the created data share. It makes difficult to detect the integrity issue and restricts dynamic policy based data share generation.

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).

