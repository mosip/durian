package io.mosip.datashare.test.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.datashare.dto.PolicyManagerResponseDto;
import io.mosip.datashare.dto.PolicyResponseDto;
import io.mosip.datashare.exception.ApiNotAccessibleException;
import io.mosip.datashare.exception.PolicyException;
import io.mosip.datashare.util.PolicyUtil;
import io.mosip.kernel.core.exception.ServiceError;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*",
		"com.sun.org.apache.xalan.*" })
public class PolicyUtilTest {
	/** The environment. */
	@Mock
	private Environment environment;

	/** The rest template. */
	@Mock
	private RestTemplate restTemplate;

	/** The mapper. */
	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	PolicyUtil policyUtil;

	private PolicyManagerResponseDto policyManagerResponseDto;

	String policyResponse;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws JsonParseException, JsonMappingException, IOException {
		ReflectionTestUtils.setField(policyUtil, "partnerPolicyUrl", "https://test/partnermanagement/v1/policies/policies/partnerId/{partnerId}/policyId/{policyId}");
		policyManagerResponseDto = new PolicyManagerResponseDto();
		PolicyResponseDto responseData = new PolicyResponseDto();
		responseData.setPolicyId("1234");
		policyManagerResponseDto.setResponse(responseData);

		ResponseEntity<String> response = new ResponseEntity<String>(policyResponse, HttpStatus.OK);
		Mockito.when(restTemplate.exchange(Mockito.any(String.class), Mockito.any(HttpMethod.class),
				Mockito.any(HttpEntity.class), Mockito.any(Class.class))).thenReturn(response);

		Mockito.when(objectMapper.readValue(policyResponse, PolicyManagerResponseDto.class))
				.thenReturn(policyManagerResponseDto);
	}

	@Test
	public void policySuccessTest() throws IOException {


		PolicyResponseDto policyResponseDto = policyUtil.getPolicyDetail("1234", "3456");
		assertEquals(policyResponseDto.getPolicyId(), "1234");

	}

	@Test(expected = PolicyException.class)
	public void testIOException() throws JsonParseException, JsonMappingException, IOException {

		Mockito.when(objectMapper.readValue(policyResponse, PolicyManagerResponseDto.class))
				.thenThrow(new IOException());
		policyUtil.getPolicyDetail("1234", "3456");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpClientException() throws JsonParseException, JsonMappingException, IOException {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");

		Mockito.when(restTemplate.exchange(Mockito.any(String.class), Mockito.any(HttpMethod.class),
				Mockito.any(HttpEntity.class), Mockito.any(Class.class))).thenThrow(httpClientErrorException);
		policyUtil.getPolicyDetail("1234", "3456");
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ApiNotAccessibleException.class)
	public void testHttpServerException() throws JsonParseException, JsonMappingException, IOException {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");

		Mockito.when(restTemplate.exchange(Mockito.any(String.class), Mockito.any(HttpMethod.class),
				Mockito.any(HttpEntity.class), Mockito.any(Class.class))).thenThrow(httpServerErrorException);
		policyUtil.getPolicyDetail("1234", "3456");
	}

	@Test(expected = PolicyException.class)
	public void policyFailureTest() throws JsonParseException, JsonMappingException, IOException {
		ServiceError error = new ServiceError();
		error.setErrorCode("PLC-GET-001");
		error.setMessage("policy error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		policyManagerResponseDto.setErrors(errors);
		policyUtil.getPolicyDetail("1234", "3456");
	}
}
