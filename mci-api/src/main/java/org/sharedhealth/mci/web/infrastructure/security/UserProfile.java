package org.sharedhealth.mci.web.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.CollectionUtils;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfile {
    public static final String FACILITY_TYPE = "facility";
    public static final String PROVIDER_TYPE = "provider";
    public static final String PATIENT_TYPE = "patient";
    public static final String ADMIN_TYPE = "mci-supervisor";
    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("catchment")
    private List<String> catchments;

    public UserProfile() {
    }

    public UserProfile(String name, String id, List<String> catchments) {
        this.name = name;
        this.id = id;
        this.catchments = catchments;
    }

    public boolean isFacility() {
        return name.equalsIgnoreCase(FACILITY_TYPE);
    }

    public boolean isProvider() {
        return name.equalsIgnoreCase(PROVIDER_TYPE);
    }

    public boolean isPatient() {
        return name.equalsIgnoreCase(PATIENT_TYPE);
    }

    public boolean isAdmin() {
        return name.equalsIgnoreCase(ADMIN_TYPE);
    }

    public String getId() {
        return id;
    }

    public List<String> getCatchments() {
        return catchments;
    }

    public String getName() {
        return name;
    }

    public boolean hasCatchment(String requestedCatchment) {
        if (!CollectionUtils.isEmpty(catchments)) {
            for (String catchment : catchments) {
                if (requestedCatchment.startsWith(catchment)) {
                    return true;
                }
            }
        }
        return false;
    }
}
