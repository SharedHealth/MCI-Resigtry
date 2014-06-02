package org.mci.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Address {

    @JsonProperty("division_id")
    private String divisionId;
    @JsonProperty("district_id")
    private String districtId;
    @JsonProperty("upazilla_id")
    private String upazillaId;
    @JsonProperty("union_id")
    private String unionId;

    public String getDivisionId() {
        return divisionId;
    }

    public void setDivisionId(String divisionId) {
        this.divisionId = divisionId;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getUpazillaId() {
        return upazillaId;
    }

    public void setUpazillaId(String upazillaId) {
        this.upazillaId = upazillaId;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Address address = (Address) o;

        if (!districtId.equals(address.districtId)) return false;
        if (!divisionId.equals(address.divisionId)) return false;
        if (!unionId.equals(address.unionId)) return false;
        if (!upazillaId.equals(address.upazillaId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = divisionId.hashCode();
        result = 31 * result + districtId.hashCode();
        result = 31 * result + upazillaId.hashCode();
        result = 31 * result + unionId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Address{" +
                "divisionId='" + divisionId + '\'' +
                ", districtId='" + districtId + '\'' +
                ", upazillaId='" + upazillaId + '\'' +
                ", unionId='" + unionId + '\'' +
                '}';
    }
}
