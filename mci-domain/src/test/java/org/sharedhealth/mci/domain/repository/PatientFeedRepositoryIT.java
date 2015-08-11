package org.sharedhealth.mci.domain.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sharedhealth.mci.domain.constant.JsonConstants;
import org.sharedhealth.mci.domain.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.sharedhealth.mci.domain.constant.JsonConstants.NEW_VALUE;
import static org.sharedhealth.mci.domain.constant.JsonConstants.OLD_VALUE;
import static org.sharedhealth.mci.domain.repository.TestUtil.setupApprovalsConfig;
import static org.sharedhealth.mci.domain.repository.TestUtil.truncateAllColumnFamilies;
import static org.sharedhealth.mci.domain.util.DateUtil.parseDate;
import static org.sharedhealth.mci.domain.util.JsonMapper.readValue;
import static org.sharedhealth.mci.domain.util.JsonMapper.writeValueAsString;

public class PatientFeedRepositoryIT extends BaseRepositoryIT {

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private PatientFeedRepository feedRepository;

    private PatientData data;

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        data = createPatient();
        setupApprovalsConfig(cqlTemplate);
    }

    private PatientData createPatient() {
        PatientData data = new PatientData();
        data.setHealthId(String.valueOf(new Date().getTime()));
        data.setNationalId("1234567890123");
        data.setBirthRegistrationNumber("12345678901234567");
        data.setUid("12345678901");
        data.setGivenName("Scott");
        data.setSurName("Tiger");
        data.setDateOfBirth(parseDate("2014-12-01"));
        data.setGender("M");
        data.setOccupation("03");
        data.setEducationLevel("BA");
        data.setHouseholdCode("12345");

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setNumber("999900000");
        data.setPhoneNumber(phoneNumber);

        Address address = new Address();
        address.setAddressLine("house-10");
        address.setDivisionId("10");
        address.setDistrictId("04");
        address.setUpazilaId("09");
        address.setCityCorporationId("20");
        address.setUnionOrUrbanWardId("01");
        address.setCountryCode("050");
        data.setAddress(address);

        data.setRequester("Bahmni", "Dr. Monika");
        return data;
    }

    @Test
    public void shouldCreateUpdateLogsWhenPatientIsUpdated() {
        String healthId = patientRepository.create(data).getId();
        Date since = new Date();

        assertUpdateLogEntry(healthId, since, false);

        PatientData updateRequest = new PatientData();
        updateRequest.setHealthId(healthId);
        updateRequest.setGivenName("Harry");
        updateRequest.setAddress(new Address("99", "88", "77"));
        updateRequest.setRequester("Bahmni", "Dr. Monika");
        patientRepository.update(updateRequest, healthId);

        assertUpdateLogEntry(healthId, since, true);
    }

    @Test
    public void shouldCreateCreateLogsWhenPatientIsCreated() {
        Date since = new Date();
        String healthId = patientRepository.create(data).getId();
        assertUpdateLogEntry(healthId, since, true);
    }

    @Test
    public void shouldCreateUpdateLogsWhenAnyFieldIsUpdated() {
        String healthId = patientRepository.create(data).getId();
        Date since = new Date();
        assertUpdateLogEntry(healthId, since, false);

        PatientData updateRequest1 = new PatientData();
        updateRequest1.setHealthId(healthId);
        updateRequest1.setEducationLevel("02");
        updateRequest1.setRequester("Bahmni", "Dr. Monika");
        patientRepository.update(updateRequest1, healthId);
        assertUpdateLogEntry(healthId, since, true);

        PatientData updateRequest2 = new PatientData();
        updateRequest2.setGivenName("UpdGiv");
        updateRequest2.setSurName("UpdSur");
        updateRequest2.setConfidential("Yes");
        updateRequest2.setGender("F");
        Address newAddress = new Address("99", "88", "77");
        updateRequest2.setAddress(newAddress);
        updateRequest2.setRequester("Bahmni", "Dr. Monika");
        patientRepository.update(updateRequest2, healthId);

        PatientData acceptRequest = new PatientData();
        acceptRequest.setHealthId(healthId);
        acceptRequest.setGender("F");
        acceptRequest.setAddress(newAddress);
        acceptRequest.setRequester("Bahmni", "Dr. Monika");
        PatientData existingPatient = patientRepository.findByHealthId(healthId);
        patientRepository.processPendingApprovals(acceptRequest, existingPatient, true);

        List<PatientUpdateLog> patientUpdateLogs = feedRepository.findPatientsUpdatedSince(null, 25, null);
        assertEquals(4, patientUpdateLogs.size());

        Map<String, Map<String, Object>> changeSet1 = getChangeSet(patientUpdateLogs.get(1));
        assertNotNull(changeSet1);
        assertEquals(1, changeSet1.size());
        assertChangeSet(changeSet1, JsonConstants.EDU_LEVEL, data.getEducationLevel(), "02");

        Map<String, Map<String, Object>> changeSet2 = getChangeSet(patientUpdateLogs.get(2));
        assertNotNull(changeSet2);
        assertEquals(3, changeSet2.size());
        assertChangeSet(changeSet2, JsonConstants.GIVEN_NAME, data.getGivenName(), "UpdGiv");
        assertChangeSet(changeSet2, JsonConstants.SUR_NAME, data.getSurName(), "UpdSur");
        assertChangeSet(changeSet2, JsonConstants.CONFIDENTIAL, "No", "Yes");

        Map<String, Map<String, Object>> changeSet3 = getChangeSet(patientUpdateLogs.get(3));
        assertNotNull(changeSet3);
        assertEquals(2, changeSet3.size());
        assertChangeSet(changeSet3, JsonConstants.GENDER, data.getGender(), "F");
        assertChangeSet(changeSet3, JsonConstants.PRESENT_ADDRESS, data.getAddress(), newAddress);
    }

    private Map<String, Map<String, Object>> getChangeSet(PatientUpdateLog log) {
        return readValue(log.getChangeSet(), new TypeReference<Map<String, Map<String, Object>>>() {
        });
    }

    private void assertChangeSet(Map<String, Map<String, Object>> changeSet, String fieldName, String oldValue, String newValue) {
        assertEquals(oldValue, changeSet.get(fieldName).get(OLD_VALUE));
        assertEquals(newValue, changeSet.get(fieldName).get(NEW_VALUE));
    }

    private void assertChangeSet(Map<String, Map<String, Object>> changeSet, String fieldName, Address oldValue, Address newValue) {
        assertEquals(writeValueAsString(oldValue), writeValueAsString(changeSet.get(fieldName).get(OLD_VALUE)));
        assertEquals(writeValueAsString(newValue), writeValueAsString(changeSet.get(fieldName).get(NEW_VALUE)));
    }

    @Test
    public void shouldCreateUpdateLogWhenPresentAddressIsMarkedForApprovalAndUpdatedAfterApproval() {
        String healthId = patientRepository.create(data).getId();
        Date since = new Date();

        assertUpdateLogEntry(healthId, since, false);

        PatientData updateRequest = new PatientData();
        Address newAddress = new Address("99", "88", "77");
        updateRequest.setAddress(newAddress);
        updateRequest.setRequester("Bahmni", "Dr. Monika");
        patientRepository.update(updateRequest, healthId);

        assertUpdateLogEntry(healthId, since, false);

        PatientData updatedPatient = patientRepository.findByHealthId(healthId);
        updateRequest.setRequester("Bahmni", "Dr. Monika");
        patientRepository.processPendingApprovals(updateRequest, updatedPatient, true);

        List<PatientUpdateLog> patientUpdateLogs = feedRepository.findPatientsUpdatedSince(since, 1, null);
        assertEquals(healthId, patientUpdateLogs.get(0).getHealthId());
        assertEquals(writeValueAsString(new Requester("Bahmni", "Dr. Monika")), patientUpdateLogs.get(0).getApprovedBy());
    }

    private void assertUpdateLogEntry(String healthId, Date since, boolean shouldFind) {
        List<PatientUpdateLog> patientUpdateLogs = feedRepository.findPatientsUpdatedSince(since, 1, null);

        if (shouldFind) {
            assertEquals(healthId, patientUpdateLogs.get(0).getHealthId());
            assertEquals(buildRequestedBy(), patientUpdateLogs.get(0).getRequestedBy());
        } else {
            assertEquals(0, patientUpdateLogs.size());
        }
    }

    private String buildRequestedBy() {
        Map<String, Set<Requester>> requestedBy = new HashMap<>();
        Set<Requester> requester = new HashSet<>();
        requester.add(new Requester("Bahmni", "Dr. Monika"));
        requestedBy.put("ALL_FIELDS", requester);
        return writeValueAsString(requestedBy);
    }

    @Test
    public void shouldFindUpdateLogsUpdatedSince() {
        String healthId = patientRepository.create(data).getId();
        Date since = new Date();
        final int limit = 20;

        List<PatientUpdateLog> patientUpdateLogs = feedRepository.findPatientsUpdatedSince(since, limit, null);
        assertEquals(0, patientUpdateLogs.size());

        PatientData updateRequest = new PatientData();
        updateRequest.setHealthId(healthId);
        updateRequest.setGivenName("Update1");
        updateRequest.setRequester("Bahmni", "Dr. Monika");
        patientRepository.update(updateRequest, healthId);
        updateRequest.setGivenName("Update2");
        patientRepository.update(updateRequest, healthId);

        patientUpdateLogs = feedRepository.findPatientsUpdatedSince(since, limit, null);
        assertEquals(2, patientUpdateLogs.size());
        assertTrue(healthId.equals(patientUpdateLogs.get(0).getHealthId()));
    }

    @Test
    public void shouldFindUpdateLogsUpdatedAfterLastMarker() {
        String healthId = patientRepository.create(data).getId();
        Date since = new Date();
        final int limit = 20;

        List<PatientUpdateLog> patientUpdateLogs = feedRepository.findPatientsUpdatedSince(since, limit, null);
        assertEquals(0, patientUpdateLogs.size());

        PatientData updateRequest = new PatientData();
        updateRequest.setHealthId(healthId);
        updateRequest.setGivenName("Update1");
        updateRequest.setRequester("Bahmni", "Dr. Monika");
        patientRepository.update(updateRequest, healthId);

        patientUpdateLogs = feedRepository.findPatientsUpdatedSince(since, limit, null);
        assertEquals(1, patientUpdateLogs.size());
        assertTrue(healthId.equals(patientUpdateLogs.get(0).getHealthId()));

        final UUID marker = patientUpdateLogs.get(0).getEventId();

        updateRequest.setGivenName("Update2");
        patientRepository.update(updateRequest, healthId);
        updateRequest.setGivenName("Update3");
        patientRepository.update(updateRequest, healthId);

        patientUpdateLogs = feedRepository.findPatientsUpdatedSince(since, limit, null);
        assertEquals(3, patientUpdateLogs.size());

        patientUpdateLogs = feedRepository.findPatientsUpdatedSince(since, limit, marker);
        assertEquals(2, patientUpdateLogs.size());
    }

    @After
    public void tearDown() {
        truncateAllColumnFamilies(cqlTemplate);
    }
}