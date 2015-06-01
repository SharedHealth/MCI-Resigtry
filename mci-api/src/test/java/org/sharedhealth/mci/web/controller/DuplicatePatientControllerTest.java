package org.sharedhealth.mci.web.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.sharedhealth.mci.web.infrastructure.security.TokenAuthentication;
import org.sharedhealth.mci.web.infrastructure.security.UserInfo;
import org.sharedhealth.mci.web.infrastructure.security.UserProfile;
import org.sharedhealth.mci.web.mapper.*;
import org.sharedhealth.mci.web.service.DuplicatePatientService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.infrastructure.persistence.TestUtil.asSet;
import static org.sharedhealth.mci.web.infrastructure.security.UserInfo.*;
import static org.sharedhealth.mci.web.utils.JsonMapper.writeValueAsString;
import static org.sharedhealth.mci.web.utils.MCIConstants.DUPLICATION_ACTION_RETAIN_ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

public class DuplicatePatientControllerTest {

    private static final String FACILITY_ID = "100067";
    private static final String PROVIDER_ID = "100068";
    private static final String ADMIN_ID = "102";

    @Mock
    private LocalValidatorFactoryBean validatorFactory;
    @Mock
    private DuplicatePatientService duplicatePatientService;

    private MockMvc mockMvc;

    @Before
    public void setup() throws ParseException {
        initMocks(this);
        DuplicatePatientController duplicatePatientController = new DuplicatePatientController(duplicatePatientService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(duplicatePatientController)
                .setValidator(validatorFactory)
                .build();
        getContext().setAuthentication(new TokenAuthentication(getUserInfo(), true));
    }

    private UserInfo getUserInfo() {
        UserProfile facilityProfile = new UserProfile("facility", FACILITY_ID, asList("1020"));
        UserProfile providerProfile = new UserProfile("provider", PROVIDER_ID, asList("102030"));
        UserProfile adminProfile = new UserProfile("mci-supervisor", ADMIN_ID, asList("10"));

        return new UserInfo("102", "ABC", "abc@mail", 1, true, "111100",
                new ArrayList<>(asList(MCI_USER_GROUP, MCI_ADMIN, MCI_APPROVER, FACILITY_GROUP, PROVIDER_GROUP)),
                asList(facilityProfile, providerProfile, adminProfile));
    }

    @Test
    public void shouldFindDuplicatesByCatchment() throws Exception {
        when(duplicatePatientService.findAllByCatchment(new Catchment("102030"))).thenReturn(buildDuplicatePatientDataList());

        String url = "/patients/duplicates/catchments/102030";
        MvcResult mvcResult = mockMvc.perform(get(url).contentType(APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.results[0]").exists())
                .andExpect(jsonPath("$.results[1]").exists())
                .andExpect(jsonPath("$.results[2]").exists())
                .andExpect(jsonPath("$.results[3]").exists())
                .andExpect(jsonPath("$.results[4]").exists())
                .andExpect(jsonPath("$.results[5]").doesNotExist())

                .andExpect(jsonPath("$.results[0].hid1", is("99001")))
                .andExpect(jsonPath("$.results[0].hid2", is("99002")))
                .andExpect(jsonPath("$.results[0].reasons", is(asList("PHONE", "NID"))));
    }

    private List<DuplicatePatientData> buildDuplicatePatientDataList() {
        return asList(new DuplicatePatientData("99001", "99002", asSet("NID", "PHONE"), timeBased().toString()),
                new DuplicatePatientData("99003", "99004", asSet("NID"), timeBased().toString()),
                new DuplicatePatientData("99005", "99006", asSet("NID"), timeBased().toString()),
                new DuplicatePatientData("99007", "99008", asSet("PHONE"), timeBased().toString()),
                new DuplicatePatientData("99009", "99010", asSet("NID", "PHONE"), timeBased().toString()));
    }

    @Test(expected = Exception.class)
    public void shouldNotFindDuplicatesIfCatchmentDoesNotMatch() throws Exception {

        String url = "/patients/duplicates/catchments/12345";
        MvcResult mvcResult = mockMvc.perform(get(url).contentType(APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult));
    }

    @Test
    public void shouldMergeDuplicates() throws Exception {
        PatientData patient1 = new PatientData();
        PatientData patient2 = new PatientData();
        DuplicatePatientMergeData data = new DuplicatePatientMergeData(DUPLICATION_ACTION_RETAIN_ALL, patient1, patient2);

        String url = "/patients/duplicates";
        MvcResult mvcResult = mockMvc.perform(put(url).content(writeValueAsString(data)).contentType(APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isAccepted());

        ArgumentCaptor<DuplicatePatientMergeData> argument = ArgumentCaptor.forClass(DuplicatePatientMergeData.class);
        verify(duplicatePatientService).processDuplicates(argument.capture());

        Requester requester = new Requester(FACILITY_ID, PROVIDER_ID);
        requester.setAdmin(new RequesterDetails(ADMIN_ID));
        DuplicatePatientMergeData argValue = argument.getValue();
        assertEquals("1000", argValue.getAction());
        assertEquals(requester, argValue.getPatient1().getRequester());
        assertEquals(requester, argValue.getPatient2().getRequester());
    }
}