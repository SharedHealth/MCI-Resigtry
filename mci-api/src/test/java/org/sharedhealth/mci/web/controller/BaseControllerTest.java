package org.sharedhealth.mci.web.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.sharedhealth.mci.web.handler.MCIMultiResponse;
import org.sharedhealth.mci.web.handler.MCIResponse;
import org.sharedhealth.mci.web.mapper.Address;
import org.sharedhealth.mci.web.mapper.PatientData;
import org.sharedhealth.mci.web.mapper.PatientSummaryData;
import org.sharedhealth.mci.web.mapper.Relation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.sharedhealth.mci.web.infrastructure.persistence.PatientRepositoryConstants.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class BaseControllerTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("MCICassandraTemplate")
    protected CassandraOperations cqlTemplate;

    protected MockMvc mockMvc;
    protected PatientData patientData;
    protected ObjectMapper mapper = new ObjectMapper();
    public static final String API_END_POINT = "/api/v1/patients";
    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";

    protected MvcResult createPatient(String json) throws Exception {
        return mockMvc.perform(post(API_END_POINT).accept(APPLICATION_JSON).content(json).contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    protected MCIMultiResponse getMciMultiResponse(MvcResult result) {
        final ResponseEntity asyncResult = (ResponseEntity<MCIMultiResponse>) result.getAsyncResult();

        return (MCIMultiResponse) asyncResult.getBody();
    }

    protected MCIResponse getMciResponse(MvcResult result) {
        final ResponseEntity asyncResult = (ResponseEntity<MCIResponse>) result.getAsyncResult();

        return (MCIResponse) asyncResult.getBody();
    }

    protected PatientData getPatientObjectFromString(String json) throws Exception {
        return mapper.readValue(json, PatientData.class);
    }

    protected PatientSummaryData getPatientSummaryObjectFromString(String json) throws Exception {
        return mapper.readValue(json, PatientSummaryData.class);
    }

    protected Address getAddressObjectFromString(String json) throws Exception {
        return mapper.readValue(json, Address.class);
    }

    protected boolean isRelationsEqual(List<Relation> original, List<Relation> patient) {
        return original.containsAll(patient) && patient.containsAll(original);
    }

    protected void assertPatientEquals(PatientData original, PatientData patient) {
        synchronizeAutoGeneratedFields(original, patient);
        Assert.assertEquals(original, patient);
    }

    protected void synchronizeAutoGeneratedFields(PatientData original, PatientData patient) {
        original.setHealthId(patient.getHealthId());
        original.setCreatedAt(patient.getCreatedAt());
        original.setUpdatedAt(patient.getUpdatedAt());
    }

    protected PatientData getPatientObjectFromResponse(ResponseEntity asyncResult) throws Exception {
        return getPatientObjectFromString(mapper.writeValueAsString((PatientData) asyncResult.getBody()));
    }

    @After
    public void teardown() {
        cqlTemplate.execute("truncate " + CF_PATIENT);
        cqlTemplate.execute("truncate " + CF_NID_MAPPING);
        cqlTemplate.execute("truncate " + CF_BRN_MAPPING);
        cqlTemplate.execute("truncate " + CF_UID_MAPPING);
        cqlTemplate.execute("truncate " + CF_PHONE_NUMBER_MAPPING);
        cqlTemplate.execute("truncate " + CF_NAME_MAPPING);
        cqlTemplate.execute("truncate " + CF_PENDING_APPROVAL_MAPPING);
        cqlTemplate.execute("truncate " + CF_HOUSEHOLD_CODE_MAPPING);
    }

    protected PatientData getPatientMapperObjectByHealthId(String healthId) throws Exception {
        MvcResult getResult = mockMvc.perform(get(API_END_POINT + "/" + healthId).accept(APPLICATION_JSON).contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        final ResponseEntity asyncResult = (ResponseEntity<PatientData>) getResult.getAsyncResult();


        return getPatientObjectFromResponse(asyncResult);
    }

    protected class InvalidPatient {

        @JsonProperty("nid")
        public String nationalId = "1234567890123";

        @JsonProperty("invalid_property")
        public String birthRegistrationNumber = "some thing";
    }

}
