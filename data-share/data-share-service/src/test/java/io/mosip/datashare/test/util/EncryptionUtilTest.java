package io.mosip.datashare.test.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.datashare.dto.CryptomanagerResponseDto;
import io.mosip.datashare.dto.EncryptResponseDto;
import io.mosip.datashare.dto.KeyManagerGetCertificateResponseDto;
import io.mosip.datashare.dto.KeyManagerUploadCertificateResponseDto;
import io.mosip.datashare.dto.PartnerCertDownloadResponeDto;
import io.mosip.datashare.dto.PartnerGetCertificateResponseDto;
import io.mosip.datashare.dto.UploadCertificateResponseDto;
import io.mosip.datashare.exception.DataEncryptionFailureException;
import io.mosip.datashare.util.EncryptionUtil;
import io.mosip.datashare.util.RestUtil;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.util.CryptoUtil;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest(value = CryptoUtil.class)
public class EncryptionUtilTest {

	/** The environment. */
	@Mock
	private Environment environment;

	/** The rest template. */
	@Mock
	private RestUtil restUtil;

	/** The mapper. */
	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	EncryptionUtil encryptionUtil;

	private CryptomanagerResponseDto cryptomanagerResponseDto;
	private KeyManagerGetCertificateResponseDto certificateResponseobj;
	private KeyManagerUploadCertificateResponseDto uploadCertificateResponseobj;
	private PartnerGetCertificateResponseDto partnerCertificateResponseObj;

	String response;
	byte[] sample;

	String test;

	@Before
	public void setUp() throws Exception {
		test = "testdata";
		sample = test.getBytes();
		cryptomanagerResponseDto = new CryptomanagerResponseDto();
		EncryptResponseDto responseData = new EncryptResponseDto();
		responseData.setData(test);
		cryptomanagerResponseDto.setResponse(responseData);
		certificateResponseobj =new KeyManagerGetCertificateResponseDto();
		ServiceError error = new ServiceError("KER-KMS-002","ApplicationId not found in Key Policy");
		List<ServiceError> errors=new ArrayList<>();
		errors.add(error);
		certificateResponseobj.setErrors(errors);
		uploadCertificateResponseobj=new KeyManagerUploadCertificateResponseDto();
		UploadCertificateResponseDto uploadCertificateResponseDto=new UploadCertificateResponseDto();
		uploadCertificateResponseDto.setStatus("uploaded");
		uploadCertificateResponseobj.setResponse(uploadCertificateResponseDto);
		partnerCertificateResponseObj=new PartnerGetCertificateResponseDto();
		PartnerCertDownloadResponeDto partnerCertDownloadResponeDto=new PartnerCertDownloadResponeDto();
		partnerCertDownloadResponeDto.setCertificateData(test);
		partnerCertificateResponseObj.setResponse(partnerCertDownloadResponeDto);
		response = "testdata";

		Mockito.when(restUtil.postApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(response);
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);
		Mockito.when(restUtil.getApi(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);
		
		Mockito.when(objectMapper.readValue(response, CryptomanagerResponseDto.class))
				.thenReturn(cryptomanagerResponseDto);
		Mockito.when(objectMapper.readValue(response, KeyManagerGetCertificateResponseDto.class))
		.thenReturn(certificateResponseobj);
		Mockito.when(objectMapper.readValue(response, KeyManagerUploadCertificateResponseDto.class))
		.thenReturn(uploadCertificateResponseobj);
		Mockito.when(objectMapper.readValue(response, PartnerGetCertificateResponseDto.class))
		.thenReturn(partnerCertificateResponseObj);
		
		Mockito.when(environment.getProperty("mosip.data.share.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		

	}

	@Test
	public void encryptionSuccessTest() throws IOException {


		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.encodeBase64(Mockito.any())).thenReturn(test);
		byte[] encryptedData = encryptionUtil.encryptData(sample, "112");
		String resultData = IOUtils.toString(encryptedData);
		assertEquals(test, resultData);

	}
	
	@Test(expected = DataEncryptionFailureException.class)
	public void testIOException() throws JsonParseException, JsonMappingException, IOException {
		String test = "testdata";
		byte[] sample = test.getBytes();
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.decodeBase64(Mockito.anyString())).thenReturn(sample);
		Mockito.when(objectMapper.readValue(response, CryptomanagerResponseDto.class)).thenThrow(new IOException());
		encryptionUtil.encryptData(sample, "");

	}


	@Test(expected = DataEncryptionFailureException.class)
	public void encryptionFailureTest() throws JsonParseException, JsonMappingException, IOException {
		ServiceError error = new ServiceError();
		error.setErrorCode("KER-KEY-001");
		error.setMessage("encryption error error");
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		cryptomanagerResponseDto.setErrors(errors);


		encryptionUtil.encryptData(sample, "");
	}
}
