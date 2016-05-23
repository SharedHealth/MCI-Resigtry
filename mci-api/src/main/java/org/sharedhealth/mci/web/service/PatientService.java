package org.sharedhealth.mci.web.service;


import org.sharedhealth.mci.domain.config.MCIProperties;
import org.sharedhealth.mci.domain.exception.Forbidden;
import org.sharedhealth.mci.domain.model.*;
import org.sharedhealth.mci.domain.repository.PatientFeedRepository;
import org.sharedhealth.mci.domain.repository.PatientRepository;
import org.sharedhealth.mci.domain.util.RxMap;
import org.sharedhealth.mci.web.exception.InsufficientPrivilegeException;
import org.sharedhealth.mci.web.mapper.PendingApprovalListResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.OrgHealthId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.functions.Func1;

import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sharedhealth.mci.domain.constant.JsonConstants.HID;
import static org.sharedhealth.mci.domain.constant.JsonConstants.RELATIONS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class PatientService {

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);
    private static final int PER_PAGE_MAXIMUM_LIMIT = 25;
    public static final String PER_PAGE_MAXIMUM_LIMIT_NOTE = "There are more record for this search criteria. " +
            "Please narrow down your search";
    public static final String MESSAGE_INSUFFICIENT_PRIVILEGE = "insufficient.privilege";
    public static final String MESSAGE_INVALID_PENDING_APPROVALS = "invalid.pending.approvals";
    public static final String MESSAGE_PENDING_APPROVALS_MISMATCH = "pending.approvals.mismatch";
    public static final int CREATED = 201;

    private PatientRepository patientRepository;
    private PatientFeedRepository feedRepository;
    private SettingService settingService;
    private PatientHealthIdService patientHealthIdService;
    private MCIProperties mciProperties;

    @Autowired
    public PatientService(PatientRepository patientRepository,
                          PatientFeedRepository feedRepository,
                          SettingService settingService,
                          PatientHealthIdService patientHealthIdService, MCIProperties mciProperties) {
        this.patientRepository = patientRepository;
        this.feedRepository = feedRepository;
        this.settingService = settingService;
        this.patientHealthIdService = patientHealthIdService;
        this.mciProperties = mciProperties;
    }

    public Observable<MCIResponse> createPatientForMCI(PatientData patient) throws InterruptedException {
        logger.debug("Create patient");
        PatientData existingPatient = findPatientByMultipleIds(patient);
        if (existingPatient != null) {
            return this.update(patient, existingPatient.getHealthId());
        }

        Observable<MCIResponse> createPatientObservable;
        setHealthIdAssignor(patient);
        MciHealthId nextMciHealthId = null;
        try {
            nextMciHealthId = patientHealthIdService.getNextHealthId();
        } catch (NoSuchElementException e) {
            MCIResponse mciResponse = new MCIResponse("Can not create patient as there is no hid available in MCI to assign", BAD_REQUEST);
            return Observable.just(mciResponse);
        }
        patient.setHealthId(nextMciHealthId.getHid());
        createPatientObservable = patientRepository.create(patient);
        return createPatientObservable.flatMap(createMCIPatientCallback(nextMciHealthId),
                RxMap.<MCIResponse>logAndForwardError(logger), RxMap.<MCIResponse>completeResponds());
    }

    public Observable<MCIResponse> createPatientForOrg(PatientData patient, String facilityId) {
        String healthId = patient.getHealthId();
        logger.debug(String.format("Creating patient for Organization [%s] with HealthID [%s]", facilityId, healthId));
        PatientData existingPatient = findPatientByMultipleIds(patient);
        if (existingPatient != null) {
            return this.update(patient, existingPatient.getHealthId());
        }
        if (isMCIHealthId(healthId)) {
            MCIResponse mciResponse = new MCIResponse("The HealthId is not for given organization", BAD_REQUEST);
            return Observable.just(mciResponse);
        }
        if (isInvalidOrgHID(healthId)) {
            MCIResponse mciResponse = new MCIResponse("The HealthId for patient is not valid", BAD_REQUEST);
            return Observable.just(mciResponse);
        }
        final OrgHealthId orgHealthId = patientHealthIdService.findOrgHealthId(healthId);
        MCIResponse validationResponse = validateHealthId(orgHealthId, facilityId);
        if (null != validationResponse) {
            return Observable.just(validationResponse);
        }
        Observable<MCIResponse> createPatientObservable = patientRepository.create(patient);
        return createPatientObservable.flatMap(new Func1<MCIResponse, Observable<MCIResponse>>() {
            @Override
            public Observable<MCIResponse> call(MCIResponse mciResponse) {
                patientHealthIdService.markOrgHealthIdUsed(orgHealthId);
                return Observable.just(mciResponse);
            }
        }, RxMap.<MCIResponse>logAndForwardError(logger), RxMap.<MCIResponse>completeResponds());
    }

    private Func1<MCIResponse, Observable<MCIResponse>> createMCIPatientCallback(final MciHealthId nextMciHealthId) {
        return new Func1<MCIResponse, Observable<MCIResponse>>() {
            @Override
            public Observable<MCIResponse> call(MCIResponse mciResponse) {
                if (CREATED == mciResponse.getHttpStatus()) {
                    patientHealthIdService.markUsed(nextMciHealthId);
                } else {
                    patientHealthIdService.putBackHealthId(nextMciHealthId);
                }
                return Observable.just(mciResponse);
            }
        };
    }

    private boolean isMCIHealthId(String healthId) {
        String hidWithoutChecksum = healthId.substring(0, healthId.length() - 1);
        Long hidToCompare = Long.valueOf(hidWithoutChecksum);
        return hidToCompare > mciProperties.getMciStartHid() && hidToCompare < mciProperties.getMciEndHid();
    }

    private boolean isInvalidOrgHID(String healthId) {
        String hidWithoutChecksum = healthId.substring(0, healthId.length() - 1);
        return hidWithoutChecksum.matches(mciProperties.getOtherOrgInvalidHidPattern());
    }

    private MCIResponse validateHealthId(OrgHealthId orgHealthId, String facilityId) {
        if (null == orgHealthId) {
            return new MCIResponse("The HealthId is not present", BAD_REQUEST);
        }
        if (!facilityId.equals(orgHealthId.getAllocatedFor())) {
            return new MCIResponse("The HealthId is not for given organization", BAD_REQUEST);
        }
        if (orgHealthId.isUsed()) {
            return new MCIResponse("The HealthId is already used", BAD_REQUEST);
        }
        return null;
    }

    private void setHealthIdAssignor(PatientData patient) {
        if (null == patient.getHealthId()) {
            patient.setAssignedBy("MCI");
        }
    }

    PatientData findPatientByMultipleIds(PatientData patient) {
        if (!this.containsMultipleIds(patient)) {
            return null;
        }

        SearchQuery query = new SearchQuery();
        query.setNid(patient.getNationalId());
        List<PatientData> patientsByNid = findAllByQuery(query);

        query = new SearchQuery();
        query.setBin_brn(patient.getBirthRegistrationNumber());
        List<PatientData> patientsByBrn = findAllByQuery(query);
        if (isEmpty(patientsByNid) && isEmpty(patientsByBrn)) {
            return null;
        }

        List<PatientData> matchingPatients = (List<PatientData>) intersection(patientsByNid, patientsByBrn);
        if (isNotEmpty(matchingPatients)) {
            return matchingPatients.get(0);
        }
        matchingPatients = (List<PatientData>) union(patientsByNid, patientsByBrn);

        query = new SearchQuery();
        query.setUid(patient.getUid());
        List<PatientData> patientsByUid = findAllByQuery(query);

        matchingPatients = (List<PatientData>) intersection(matchingPatients, patientsByUid);
        if (isNotEmpty(matchingPatients)) {
            return matchingPatients.get(0);
        }
        return null;
    }

    boolean containsMultipleIds(PatientData patient) {
        int count = 0;
        if (isNotBlank(patient.getNationalId())) {
            count++;
        }
        if (isNotBlank(patient.getBirthRegistrationNumber())) {
            count++;
        }
        if (isNotBlank(patient.getUid())) {
            count++;
        }
        return count > 1;
    }

    public Observable<MCIResponse> update(PatientData patient, String healthId) {
        return patientRepository.update(patient, healthId);
    }

    public PatientData findByHealthId(String healthId) {
        return patientRepository.findByHealthId(healthId);
    }

    public List<PatientData> findAllByQuery(SearchQuery searchQuery) {
        return patientRepository.findAllByQuery(searchQuery);
    }

    public List<PatientSummaryData> findAllSummaryByQuery(SearchQuery searchQuery) {
        return patientRepository.findAllSummaryByQuery(searchQuery);
    }

    public List<PatientData> findAllByCatchment(Catchment catchment, Date since, UUID lastMarker) {
        return patientRepository.findAllByCatchment(catchment, since, lastMarker, getPerPageMaximumLimit());
    }

    public int getPerPageMaximumLimit() {
        Integer limit = settingService.getSettingAsIntegerByKey("PER_PAGE_MAXIMUM_LIMIT");

        if (limit == null) {
            return PER_PAGE_MAXIMUM_LIMIT;
        }
        return limit;
    }

    public String getPerPageMaximumLimitNote() {
        String note = settingService.getSettingAsStringByKey("PER_PAGE_MAXIMUM_LIMIT_NOTE");

        if (note == null) {
            return PER_PAGE_MAXIMUM_LIMIT_NOTE;
        }
        return note;
    }

    public List<PendingApprovalListResponse> findPendingApprovalList(Catchment catchment, UUID after, UUID before, int limit) {
        List<PendingApprovalListResponse> pendingApprovals = new ArrayList<>();
        List<PendingApprovalMapping> mappings = patientRepository.findPendingApprovalMapping(catchment, after, before, limit);
        if (isNotEmpty(mappings)) {
            for (PendingApprovalMapping mapping : mappings) {
                PatientData patient = patientRepository.findByHealthId(mapping.getHealthId());
                pendingApprovals.add(buildPendingApprovalListResponse(patient, mapping.getLastUpdated()));
            }
        }
        return pendingApprovals;
    }

    private PendingApprovalListResponse buildPendingApprovalListResponse(PatientData patient, UUID lastUpdated) {
        PendingApprovalListResponse pendingApproval = new PendingApprovalListResponse();
        pendingApproval.setHealthId(patient.getHealthId());
        pendingApproval.setGivenName(patient.getGivenName());
        pendingApproval.setSurname(patient.getSurName());
        pendingApproval.setLastUpdated(lastUpdated);
        return pendingApproval;
    }

    public TreeSet<PendingApproval> findPendingApprovalDetails(String healthId, Catchment catchment) {
        logger.debug(format("find pending approval for healthId: %s and for catchment: %s", healthId, catchment.toString()));
        PatientData patient = this.findByHealthId(healthId);
        if (patient == null) {
            return null;
        }
        if (null != patient.isActive() && !patient.isActive()) {
            throw new Forbidden("patient is already marked inactive");
        }
        verifyCatchment(patient, catchment);
        TreeSet<PendingApproval> pendingApprovals = patient.getPendingApprovals();
        if (isNotEmpty(pendingApprovals)) {
            for (PendingApproval pendingApproval : pendingApprovals) {
                pendingApproval.setCurrentValue(patient.getValue(pendingApproval.getName()));
            }
        }
        return pendingApprovals;
    }

    public String processPendingApprovals(PatientData requestData, Catchment catchment, boolean shouldAccept) {
        logger.debug(format("process pending approval for healthId: %s and for catchment: %s", requestData.getHealthId(), catchment
                .toString()));
        PatientData existingPatient = this.findByHealthId(requestData.getHealthId());
        if (null != existingPatient.isActive() && !existingPatient.isActive()) {
            throw new Forbidden("patient is already marked inactive");
        }
        verifyCatchment(existingPatient, catchment);
        verifyPendingApprovalDetails(requestData, existingPatient);
        return patientRepository.processPendingApprovals(requestData, existingPatient, shouldAccept);
    }

    public List<PatientUpdateLog> findPatientsUpdatedSince(Date since, UUID lastMarker) {
        return feedRepository.findPatientsUpdatedSince(since, getPerPageMaximumLimit(), lastMarker);
    }

    private void verifyCatchment(PatientData patient, Catchment catchment) {
        if (!patient.belongsTo(catchment)) {
            throw new InsufficientPrivilegeException(MESSAGE_INSUFFICIENT_PRIVILEGE);
        }
    }


    private void verifyPendingApprovalDetails(PatientData patient, PatientData existingPatient) {
        if (isEmpty(existingPatient.getPendingApprovals())) {
            throw new IllegalArgumentException(MESSAGE_INVALID_PENDING_APPROVALS);
        }
        List<String> fieldNames = patient.findNonEmptyFieldNames();
        fieldNames.remove(HID);

        for (PendingApproval pendingApproval : existingPatient.getPendingApprovals()) {
            String fieldName = pendingApproval.getName();
            Object value = patient.getValue(fieldName);

            if (value != null && fieldName.equals(RELATIONS) && !pendingApproval.compareRelation(value)) {
                throw new IllegalArgumentException(MESSAGE_PENDING_APPROVALS_MISMATCH);
            }
            if (value != null && !pendingApproval.contains(value) && !fieldName.equals(RELATIONS)) {
                throw new IllegalArgumentException(MESSAGE_PENDING_APPROVALS_MISMATCH);
            }
            fieldNames.remove(fieldName);
        }
        if (isNotEmpty(fieldNames)) {
            throw new IllegalArgumentException(MESSAGE_PENDING_APPROVALS_MISMATCH);
        }
    }
}
