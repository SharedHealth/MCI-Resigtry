package org.sharedhealth.mci.web.model;

import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;

import static org.sharedhealth.mci.web.infrastructure.persistence.PatientQueryBuilder.*;
import static org.springframework.cassandra.core.PrimaryKeyType.CLUSTERED;
import static org.springframework.cassandra.core.PrimaryKeyType.PARTITIONED;

@Table(value = CF_PHONE_NUMBER_MAPPING)
public class PhoneNumberMapping {

    @PrimaryKeyColumn(name = PHONE_NO, ordinal = 0, type = PARTITIONED)
    private String phoneNumber;

    @PrimaryKeyColumn(name = HEALTH_ID, ordinal = 1, type = CLUSTERED)
    private String healthId;

    public PhoneNumberMapping(String phoneNumber, String healthId) {
        this.phoneNumber = phoneNumber;
        this.healthId = healthId;
    }
}