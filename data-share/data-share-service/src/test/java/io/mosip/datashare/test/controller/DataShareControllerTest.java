package io.mosip.datashare.test.controller;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import io.mosip.datashare.controller.DataShareController;
import io.mosip.datashare.dto.DataShare;
import io.mosip.datashare.service.DataShareService;
import io.mosip.datashare.test.TestBootApplication;
import io.mosip.datashare.test.config.TestConfig;

/*@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc*/
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource(locations = "classpath:application.properties")
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@SpringBootTest(classes = TestBootApplication.class)
@AutoConfigureMockMvc
public class DataShareControllerTest {



	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Mock
	private DataShareService dataShareService;

	@InjectMocks
	private DataShareController dataShareController;

	MockMultipartFile multiPartFile;

	@Mock
	Environment env;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.mockMvc = MockMvcBuilders.standaloneSetup(dataShareController).build();
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("test.txt").getFile());

		InputStream inputStream = new FileInputStream(file);
		byte[] dataBytes = IOUtils.toByteArray(inputStream);
		multiPartFile = new MockMultipartFile("file", "NameOfTheFile", "multipart/form-data",
				new ByteArrayInputStream(dataBytes));

	}

	@Test
	@WithUserDetails("test")
	@Ignore
	public void testDataShareSuccess() throws Exception {
		DataShare dataShare=new DataShare();
		Mockito.when(
				dataShareService.createDataShare(Mockito.anyString(), Mockito.anyString(),
						Mockito.any(MultipartFile.class)))
				.thenReturn(dataShare);
		String sample = "Test";
		Mockito.when(env.getProperty("mosip.data.share.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

		mockMvc.perform(MockMvcRequestBuilders.multipart("/create/policyId/subcriberId").file(multiPartFile)

				.contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
				.content(sample.getBytes()))
				.andExpect(status().isOk());

	}

	@Test
	@WithUserDetails("test")
	@Ignore
	public void testGetDataShareSuccess() throws Exception {
		String sample = "Test";

		Mockito.when(dataShareService.getDataFile(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
				).thenReturn(sample.getBytes());
		
		
		
		mockMvc.perform(MockMvcRequestBuilders.get("/get/policyId/subscriberId/randomsharekey")
				.contentType(MediaType.ALL_VALUE).content(sample.getBytes()))
				.andExpect(status().isOk());

	}

	@Test
	@WithUserDetails("test")
	public void testGetDataShareWithShorKeySuccess() throws Exception {
		String sample = "Test";

		Mockito.when(dataShareService.getDataFile(Mockito.anyString()))
				.thenReturn(sample.getBytes());

		mockMvc.perform(MockMvcRequestBuilders.get("/get/randomsharekey")
				.contentType(MediaType.ALL_VALUE).content(sample.getBytes())).andExpect(status().isOk());

	}

}
