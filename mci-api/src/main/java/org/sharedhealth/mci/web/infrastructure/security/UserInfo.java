package org.sharedhealth.mci.web.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UserInfo {
    @JsonProperty("user")
    private String name;
    @JsonProperty("roles")
    private HashSet<String> roles;
    @JsonProperty("locationCode")
    private String locationCode;

    public UserInfo() {
    }

    public UserInfo(String name, List<String> roles) {
        this.name = name;
        this.roles = new HashSet<>(roles);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRoles() {
        return new ArrayList<>(roles);
    }


    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }
}
