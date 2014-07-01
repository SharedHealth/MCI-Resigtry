package org.sharedhealth.mci.web.infrastructure.persistence;

public class PatientQueryBuilder {

    public static final String HEALTH_ID = "health_id";
    public static final String NATIONAL_ID = "national_id";
    public static final String FULL_NAME_BANGLA = "full_name_bangla";
    public static final String FIRST_NAME = "first_name";
    public static final String MIDDLE_NAME = "middle_name";
    public static final String LAST_NAME = "last_name";
    public static final String DATE_OF_BIRTH = "date_of_birth";
    public static final String GENDER = "gender";
    public static final String OCCUPATION = "occupation";
    public static final String EDU_LEVEL = "edu_level";
    public static final String PRIMARY_CONTACT = "primary_contact";
    public static final String ADDRESS_LINE = "address_line";
    public static final String DIVISION_ID = "division_id";
    public static final String DISTRICT_ID = "district_id";
    public static final String UPAZILLA_ID = "upazilla_id";
    public static final String UNION_ID = "union_id";
    public static final String BIN_BRN = "bin_brn";
    public static final String UID ="uid";
    public static final String FATHERS_NAME_BANGLA ="fathers_name_bangla";
    public static final String FATHERS_FIRST_NAME ="fathers_first_name";
    public static final String FATHERS_MIDDLE_NAME ="fathers_middle_name";
    public static final String FATHERS_LAST_NAME ="fathers_last_name";
    public static final String FATHERS_UID ="fathers_uid";
    public static final String FATHERS_NID = "fathers_nid";
    public static final String FATHERS_BRN = "fathers_brn";
    public static final String MOTHERS_NAME_BANGLA = "mothers_name_bangla";
    public static final String MOTHERS_FIRST_NAME = "mothers_first_name";
    public static final String MOTHERS_MIDDLE_NAME = "mothers_middle_name";
    public static final String MOTHERS_LAST_NAME = "mothers_last_name";
    public static final String MOTHERS_UID = "mothers_uid";
    public static final String MOTHERS_NID = "mothers_nid";
    public static final String MOTHERS_BRN = "mothers_brn";
    public static final String PLACE_OF_BIRTH = "place_of_birth";
    public static final String MARITAL_STATUS = "marital_status";
    public static final String MARRIAGE_ID = "marriage_id";
    public static final String SPOUSE_NAME_BANGLA = "spouse_name_bangla";
    public static final String SPOUSE_NAME = "spouse_name";
    public static final String SPOUSE_UID_NID = "spouse_uid_nid";
    public static final String RELIGION = "religion";
    public static final String BLOOD_GROUP = "blood_group";
    public static final String NATIONALITY = "nationality";
    public static final String DISABILITY = "disability";
    public static final String ETHNICITY = "ethnicity";


    public static String getCreateQuery() {
        return String.format("INSERT INTO patient ( %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) values " +
                "('%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s','%%s' );",
                HEALTH_ID,
                NATIONAL_ID,
                BIN_BRN,
                FULL_NAME_BANGLA,
                FIRST_NAME,
                MIDDLE_NAME,
                LAST_NAME,
                DATE_OF_BIRTH,
                GENDER,
                OCCUPATION,
                EDU_LEVEL,
                PRIMARY_CONTACT,
                FATHERS_NAME_BANGLA,
                FATHERS_FIRST_NAME,
                FATHERS_MIDDLE_NAME,
                FATHERS_LAST_NAME,
                FATHERS_BRN,
                FATHERS_NID,
                FATHERS_UID,
                MOTHERS_NAME_BANGLA,
                MOTHERS_FIRST_NAME,
                MOTHERS_MIDDLE_NAME,
                MOTHERS_LAST_NAME,
                MOTHERS_BRN,
                MOTHERS_NID,
                MOTHERS_UID,
                UID,
                PLACE_OF_BIRTH,
                MARITAL_STATUS,
                MARRIAGE_ID,
                SPOUSE_NAME,
                SPOUSE_NAME_BANGLA,
                SPOUSE_UID_NID,
                RELIGION,
                BLOOD_GROUP,
                NATIONALITY,
                DISABILITY,
                ETHNICITY,
                ADDRESS_LINE,
                DIVISION_ID,
                DISTRICT_ID,
                UPAZILLA_ID,
                UNION_ID);
    }

    public static String getFindByHealthIdQuery() {
        return String.format("SELECT * FROM patient WHERE %s = '%%s'", HEALTH_ID);
    }

    public static String getFindByNationalIdQuery() {
        return String.format("SELECT * FROM patient WHERE %s = '%%s'", NATIONAL_ID);
    }
}
