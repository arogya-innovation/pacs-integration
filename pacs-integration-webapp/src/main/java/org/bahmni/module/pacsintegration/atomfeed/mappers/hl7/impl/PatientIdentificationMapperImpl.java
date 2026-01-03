package org.bahmni.module.pacsintegration.atomfeed.mappers.hl7.impl;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v25.datatype.CX;
import ca.uhn.hl7v2.model.v25.segment.PID;
import liquibase.pro.packaged.in;
import liquibase.pro.packaged.l;

import org.bahmni.module.pacsintegration.atomfeed.contract.order.OpenMRSOrderDetails;
import org.bahmni.module.pacsintegration.atomfeed.contract.order.Patient;
import org.bahmni.module.pacsintegration.atomfeed.contract.order.Person;
import org.bahmni.module.pacsintegration.atomfeed.mappers.hl7.Constants;
import org.bahmni.module.pacsintegration.atomfeed.mappers.hl7.PatientIdentificationMapper;
import org.hibernate.type.descriptor.sql.JdbcTypeFamilyInformation.Family;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PatientIdentificationMapperImpl implements PatientIdentificationMapper {

    private static final Logger logger = LoggerFactory.getLogger(PatientIdentificationMapperImpl.class);

    // @Override
    // public void map(PID pid, OpenMRSOrderDetails orderDetails) {
    //     try {
    //         mapPatientDemographics(pid, orderDetails);
    //         mapPatientId(pid, orderDetails);

    //     } catch (HL7Exception e) {
    //         logger.error("Error mapping PID segment for patient", e);
    //         throw new RuntimeException("Failed to map patient identification segment", e);
    //     }
    // }


    @Override
    public void map(PID pid, OpenMRSOrderDetails orderDetails) {
        logger.info("=== Starting PID Segment Mapping ===");
        try {
            // IMPORTANT: Map patient ID FIRST (PID-3)
            mapPatientId(pid, orderDetails);
            
            // Then map demographics (PID-5, PID-7, PID-8)
            mapPatientDemographics(pid, orderDetails);
            
            // Final verification of complete PID segment
            logger.info("=== Final PID segment after all mappings ===");
            String finalPid = pid.encode();
            logger.info("Complete PID: {}", finalPid);
            logger.info("=== End Final PID ===");

        } catch (HL7Exception e) {
            logger.error("Error mapping PID segment for patient", e);
            throw new RuntimeException("Failed to map patient identification segment", e);
        }
    }


    private void mapPatientDemographics(PID pid, OpenMRSOrderDetails orderDetails) throws DataTypeException {
    logger.info("=== Starting PID Demographics Mapping ===");
    
    Person patientPerson = orderDetails.getPatient().getPerson();
    Person.PreferredName preferredName = patientPerson.getPreferredName();

    // Log raw input data
    logger.info("Raw patient data - Given Name: {}, Middle Name: {}, Family Name: {}", 
                preferredName.getGivenName(), 
                preferredName.getMiddleName(), 
                preferredName.getFamilyName());
    logger.info("Raw patient data - Birthdate: {}, Gender: {}", 
                patientPerson.getBirthdate(), patientPerson.getGender());

    // Use placeholders if first or last name is missing
    String firstName = preferredName.getGivenName() != null && !preferredName.getGivenName().trim().isEmpty() 
                       ? preferredName.getGivenName().trim() : "UNKNOWN";
    String middleName = preferredName.getMiddleName() != null && !preferredName.getMiddleName().trim().isEmpty() 
                        ? preferredName.getMiddleName().trim() : "";
    String lastName = preferredName.getFamilyName() != null && !preferredName.getFamilyName().trim().isEmpty() 
                      ? preferredName.getFamilyName().trim() : "UNKNOWN";

    logger.info("Processed names - First: {}, Middle: {}, Last: {}", firstName, middleName, lastName);

    // PID-5: Patient Name - Format: FamilyName^GivenName^MiddleName^Suffix^Prefix
    // Using parse() is more reliable for composite fields
    try {
        // Build patient name with middle name component
        String patientName;
        if (!middleName.isEmpty()) {
            patientName = lastName + "^" + firstName + "^" + middleName;
            logger.info("Attempting to set PID-5 Patient Name (with middle): {}", patientName);
        } else {
            // Include empty component for middle name to maintain structure
            patientName = lastName + "^" + firstName + "^";
            logger.info("Attempting to set PID-5 Patient Name (no middle): {}", patientName);
        }
        
        pid.getPatientName(0).parse(patientName);
        
        logger.info("✓ Successfully set PID-5 Patient Name: {}", patientName);
        
        // Verify what was actually set
        String verifyName = pid.getPatientName(0).encode();
        logger.info("✓ PID-5 verification (encoded): {}", verifyName);
        
        // Additional verification: Check individual components
        String familyName = pid.getPatientName(0).getFamilyName().getSurname().getValue();
        String givenName = pid.getPatientName(0).getGivenName().getValue();
        String middle = pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().getValue();
        logger.info("✓ Component verification - Family: {}, Given: {}, Middle: {}", 
                    familyName, givenName, middle);
        
    } catch (HL7Exception e) {
        logger.error("✗ FAILED to parse patient name: {}^{}^{}", lastName, firstName, middleName, e);
        throw new RuntimeException("Failed to set patient name in PID segment", e);
    }

    // PID-7: Date of Birth
    try {
        pid.getDateTimeOfBirth().getTime().setValue(patientPerson.getBirthdate());
        logger.info("✓ PID-7 Date of Birth set successfully");
    } catch (Exception e) {
        logger.error("✗ FAILED to set Date of Birth", e);
    }

    // PID-8: Gender
    String gender = patientPerson.getGender() != null && !patientPerson.getGender().trim().isEmpty() 
                    ? patientPerson.getGender().trim() : "U"; // U = Unknown
    logger.info("Setting PID-8 Gender: {}", gender);
    pid.getAdministrativeSex().setValue(gender);
    logger.info("✓ PID-8 Gender set successfully");

    // Final verification: Log the complete PID segment
    try {
        String pidEncoded = pid.encode();
        logger.info("=== Complete PID segment ===");
        logger.info("{}", pidEncoded);
        logger.info("=== End PID segment ===");
        
        // Parse out PID-5 specifically for verification
        String[] pidFields = pidEncoded.split("\\|");
        if (pidFields.length > 5) {
            logger.info("PID-5 field from encoded segment: [{}]", pidFields[5]);
            if (pidFields[5] == null || pidFields[5].trim().isEmpty()) {
                logger.error("✗✗✗ CRITICAL: PID-5 is EMPTY in encoded segment! ✗✗✗");
            } else {
                logger.info("✓✓✓ PID-5 is populated: [{}] ✓✓✓", pidFields[5]);
                
                // Verify components in PID-5
                String[] nameComponents = pidFields[5].split("\\^");
                logger.info("✓ PID-5 components breakdown:");
                logger.info("  └─ Family Name (0): {}", nameComponents.length > 0 ? nameComponents[0] : "MISSING");
                logger.info("  └─ Given Name (1): {}", nameComponents.length > 1 ? nameComponents[1] : "MISSING");
                logger.info("  └─ Middle Name (2): {}", nameComponents.length > 2 ? nameComponents[2] : "EMPTY");
                logger.info("  └─ Suffix (3): {}", nameComponents.length > 3 ? nameComponents[3] : "EMPTY");
                logger.info("  └─ Prefix (4): {}", nameComponents.length > 4 ? nameComponents[4] : "EMPTY");
            }
        }
        
    } catch (HL7Exception e) {
        logger.error("✗ Could not encode PID segment for verification", e);
    }
    
    logger.info("=== Completed PID Demographics Mapping ===");
}







    private void mapPatientId(PID pid, OpenMRSOrderDetails orderDetails) throws HL7Exception {
        CX patientIdentifier = pid.getPatientIdentifierList(0);
        patientIdentifier.getIDNumber().setValue(orderDetails.getPatient().getPatientIdentifier().getIdentifier());
        patientIdentifier.getIdentifierTypeCode().setValue(Constants.PATIENT_IDENTIFIER_TYPE_CODE);
        patientIdentifier.getAssigningAuthority().parse(Constants.PATIENT_IDENTIFIER_ASSIGNING_AUTHORITY);

    }


    
}
