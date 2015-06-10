package org.sharedhealth.mci.web.infrastructure.dedup;

import org.sharedhealth.mci.web.infrastructure.persistence.PatientRepository;
import org.sharedhealth.mci.web.mapper.PatientData;
import org.sharedhealth.mci.web.mapper.SearchQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DuplicatePatientUidRule extends DuplicatePatientRule {

    private final String reason;

    @Autowired
    public DuplicatePatientUidRule(PatientRepository patientRepository) {
        super(patientRepository);
        this.reason = DUPLICATE_REASON_UID;
    }

    @Override
    protected SearchQuery buildSearchQuery(PatientData patient) {
        SearchQuery query = new SearchQuery();
        query.setUid(patient.getUid());
        return query;
    }

    @Override
    protected String getReason() {
        return reason;
    }
}
