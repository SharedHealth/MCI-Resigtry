package org.sharedhealth.mci.web.service;

import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.infrastructure.persistence.FacilityRepository;
import org.sharedhealth.mci.web.infrastructure.registry.FacilityRegistryClient;
import org.sharedhealth.mci.web.mapper.FacilityResponse;
import org.sharedhealth.mci.web.model.Facility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FacilityService {

    private FacilityRepository facilityRepository;
    private FacilityRegistryClient client;
    private MCIProperties mciProperties;

    @Autowired
    public FacilityService(FacilityRepository facilityRepository, FacilityRegistryClient facilityRegistryClient,
                           MCIProperties mciProperties) {
        this.facilityRepository = facilityRepository;
        this.client = facilityRegistryClient;
        this.mciProperties = mciProperties;
    }

    public Facility find(String facilityId) {
        Facility facility = facilityRepository.find(facilityId);
        if (facility == null) {
            FacilityResponse facilityResponse = client.find(facilityId);
            if (facilityResponse != null) {
                facility = map(facilityResponse);
                facilityRepository.save(facility, mciProperties.getFrCacheTtl());
            }
        }
        return facility;
    }

    Facility map(FacilityResponse facilityResponse) {
        if (facilityResponse == null) {
            return null;
        }
        Facility facility = new Facility();
        facility.setId(facilityResponse.getId());
        facility.setName(facilityResponse.getName());
        facility.setType(facilityResponse.getType());
        facility.setCatchments(facilityResponse.getCatchments());
        facility.setLocation(facilityResponse.getGeoCode());
        return facility;
    }
}
