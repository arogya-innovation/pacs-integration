package org.bahmni.module.pacsintegration.services.impl;

import org.bahmni.module.pacsintegration.services.StudyInstanceUIDGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class StudyInstanceUIDGeneratorImpl implements StudyInstanceUIDGenerator {
        private static final Logger logger = LoggerFactory.getLogger(ImagingStudyServiceImpl.class);


    @Value("${study.instance.uid.prefix:1.2.826.0.1.3680043.8.498}")
    private String studyInstanceUIDPrefix;

    @Override
    public String generateStudyInstanceUID(String orderNumber, Date dateCreated) {

        int orderHash = Math.abs(orderNumber.hashCode());
        logger.info(orderNumber);
                logger.info("Inside generateStudyInstanceUID - orderNumber: {}, dateCreated: {}, generatedUID: {}", orderNumber, dateCreated, studyInstanceUIDPrefix + "." + dateCreated.getTime() + '.' + orderHash);

        return studyInstanceUIDPrefix + "." + dateCreated.getTime() + '.' + orderHash;
    }
}
