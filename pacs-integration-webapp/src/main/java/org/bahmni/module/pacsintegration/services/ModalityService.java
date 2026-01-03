// package org.bahmni.module.pacsintegration.services;

// import ca.uhn.hl7v2.AcknowledgmentCode;
// import ca.uhn.hl7v2.DefaultHapiContext;
// import ca.uhn.hl7v2.HL7Exception;
// import ca.uhn.hl7v2.HapiContext;
// import ca.uhn.hl7v2.app.Connection;
// import ca.uhn.hl7v2.app.Initiator;
// import ca.uhn.hl7v2.llp.LLPException;
// import ca.uhn.hl7v2.model.AbstractMessage;
// import ca.uhn.hl7v2.model.Message;
// import ca.uhn.hl7v2.model.v25.message.ACK;
// import ca.uhn.hl7v2.model.v25.message.ORR_O02;
// import ca.uhn.hl7v2.parser.PipeParser;

// import org.bahmni.module.pacsintegration.exception.ModalityException;
// import org.bahmni.module.pacsintegration.model.Modality;
// import org.bahmni.module.pacsintegration.repository.OrderTypeRepository;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

// import java.io.IOException;

// @Component
// public class ModalityService {

//     private static final Logger logger = LoggerFactory.getLogger(ModalityService.class);

//     @Autowired
//     private OrderTypeRepository orderTypeRepository;

// public String sendMessage(AbstractMessage message, String orderType)
//         throws HL7Exception, LLPException, IOException {

//     logger.info("Sending HL7 message for orderType: {}", orderType);

//     Modality modality = orderTypeRepository.getByName(orderType).getModality();
//     logger.info("Resolved modality: {} ({}:{})",
//             modality.getName(),
//             modality.getIp(),
//             modality.getPort());

//     // LOG THE COMPLETE MESSAGE BEFORE SENDING
//     try {
//         String encodedMessage = message.encode();
//         logger.info("=== COMPLETE HL7 MESSAGE BEING SENT ===");
//         logger.info(encodedMessage);
//         logger.info("=== END COMPLETE HL7 MESSAGE ===");
        
//         // Parse and verify PID segment
//         String[] segments = encodedMessage.split("\r");
//         for (String segment : segments) {
//             if (segment.startsWith("PID")) {
//                 logger.info(">>> PID SEGMENT IN MESSAGE: {}", segment);
//                 String[] fields = segment.split("\\|", -1);  // -1 to keep empty fields
//                 logger.info(">>> PID-3 (Patient ID): [{}]", fields.length > 3 ? fields[3] : "EMPTY");
//                 logger.info(">>> PID-5 (Patient Name): [{}]", fields.length > 5 ? fields[5] : "EMPTY");
//             }
//             if (segment.startsWith("MSH")) {
//                 logger.info(">>> MSH SEGMENT: {}", segment);
//             }
//         }
//     } catch (Exception e) {
//         logger.error("Failed to log message before sending", e);
//     }

//     // NOW SEND THE MESSAGE
//     Message response = post(modality, message);
//     String responseMessage = parseResponse(response);

//     logger.debug("HL7 Response received:\n{}", responseMessage);

//     if (response instanceof ORR_O02) {
//         ORR_O02 acknowledgment = (ORR_O02) response;
//         String acknowledgmentCode = acknowledgment.getMSA().getAcknowledgmentCode().getValue();

//         logger.info("Received ORR_O02 acknowledgment: {}", acknowledgmentCode);
//         processAcknowledgement(modality, responseMessage, acknowledgmentCode);

//     } else if (response instanceof ACK) {
//         ACK acknowledgment = (ACK) response;
//         String acknowledgmentCode = acknowledgment.getMSA().getAcknowledgmentCode().getValue();

//         logger.info("Received ACK acknowledgment: {}", acknowledgmentCode);
//         processAcknowledgement(modality, responseMessage, acknowledgmentCode);

//     } else {
//         logger.error("Unsupported HL7 response type: {}", response.getClass().getSimpleName());
//         throw new ModalityException(responseMessage, modality);
//     }

//     logger.info("HL7 message successfully processed for orderType: {}", orderType);

//     return responseMessage;
// }

//     Message post(Modality modality, Message requestMessage)
//             throws LLPException, IOException, HL7Exception {

//         Connection connection = null;
//         try {
//             logger.info("Opening HL7 connection to {}:{}",
//                     modality.getIp(), modality.getPort());

//             HapiContext hapiContext = new DefaultHapiContext();
//             connection = hapiContext.newClient(modality.getIp(), modality.getPort(), false);

//             Initiator initiator = connection.getInitiator();
//             logger.debug("Sending HL7 message...");
//             return initiator.sendAndReceive(requestMessage);

//         } catch (Exception e) {
//             logger.error("Error while sending HL7 message to modality", e);
//             throw e;
//         } finally {
//             if (connection != null) {
//                 connection.close();
//                 logger.debug("HL7 connection closed");
//             }
//         }
//     }

//     String parseResponse(Message response) throws HL7Exception {
//         logger.debug("Parsing HL7 response");
//         return new PipeParser().encode(response);
//     }

//     private void processAcknowledgement(Modality modality,
//                                         String responseMessage,
//                                         String acknowledgmentCode) {

//         if (!AcknowledgmentCode.AA.toString().equals(acknowledgmentCode)) {
//             logger.error("Negative acknowledgment from modality {}: {}",
//                     modality.getName(), acknowledgmentCode);
//             throw new ModalityException(responseMessage, modality);
//         }

//         logger.info("Acknowledgment successful from modality: {}", modality.getName());
//     }
// }


package org.bahmni.module.pacsintegration.services;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ACK;
import ca.uhn.hl7v2.model.v25.message.ORR_O02;
import ca.uhn.hl7v2.parser.PipeParser;

import org.bahmni.module.pacsintegration.exception.ModalityException;
import org.bahmni.module.pacsintegration.model.Modality;
import org.bahmni.module.pacsintegration.repository.OrderTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ModalityService {

    private static final Logger logger = LoggerFactory.getLogger(ModalityService.class);

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    public String sendMessage(AbstractMessage message, String orderType)
            throws HL7Exception, LLPException, IOException {

        logger.info("Sending HL7 message for orderType: {}", orderType);

        Modality modality = orderTypeRepository.getByName(orderType).getModality();
        logger.info("Resolved modality: {} ({}:{})",
                modality.getName(),
                modality.getIp(),
                modality.getPort());

        // *** CRITICAL: LOG THE COMPLETE MESSAGE BEFORE SENDING ***
        try {
            String encodedMessage = message.encode();
            logger.info("╔═══════════════════════════════════════════════════════════════╗");
            logger.info("║   COMPLETE HL7 MESSAGE ABOUT TO BE SENT TO DCM4CHEE          ║");
            logger.info("╚═══════════════════════════════════════════════════════════════╝");
            
            // Log each segment on its own line for readability
            String[] segments = encodedMessage.split("\r");
            for (String segment : segments) {
                logger.info("  {}", segment);
                
                // Highlight PID segment
                if (segment.startsWith("PID")) {
                    logger.info("  ┌─ PID SEGMENT BREAKDOWN ─────────────────────────────");
                    String[] fields = segment.split("\\|", -1);
                    logger.info("  │ PID-1 (Set ID):         [{}]", fields.length > 1 ? fields[1] : "EMPTY");
                    logger.info("  │ PID-2 (Patient ID):     [{}]", fields.length > 2 ? fields[2] : "EMPTY");
                    logger.info("  │ PID-3 (Patient ID List):[{}]", fields.length > 3 ? fields[3] : "EMPTY");
                    logger.info("  │ PID-4 (Alt Patient ID): [{}]", fields.length > 4 ? fields[4] : "EMPTY");
                    logger.info("  │ PID-5 (Patient Name):   [{}]", fields.length > 5 ? fields[5] : "EMPTY");
                    logger.info("  │ PID-6 (Mother's Name):  [{}]", fields.length > 6 ? fields[6] : "EMPTY");
                    logger.info("  │ PID-7 (Date of Birth):  [{}]", fields.length > 7 ? fields[7] : "EMPTY");
                    logger.info("  │ PID-8 (Gender):         [{}]", fields.length > 8 ? fields[8] : "EMPTY");
                    logger.info("  └─────────────────────────────────────────────────────");
                    
                    // Check if PID-5 is actually empty
                    if (fields.length <= 5 || fields[5].trim().isEmpty()) {
                        logger.error("  ✗✗✗ CRITICAL ERROR: PID-5 IS EMPTY IN THE MESSAGE! ✗✗✗");
                    } else {
                        logger.info("  ✓✓✓ PID-5 is populated: [{}] ✓✓✓", fields[5]);
                    }
                }
            }
            
            logger.info("╔═══════════════════════════════════════════════════════════════╗");
            logger.info("║   END OF HL7 MESSAGE                                          ║");
            logger.info("╚═══════════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            logger.error("Failed to log message before sending", e);
        }

        // NOW SEND THE MESSAGE
        Message response = post(modality, message);
        String responseMessage = parseResponse(response);

        logger.debug("HL7 Response received:\n{}", responseMessage);

        if (response instanceof ORR_O02) {
            ORR_O02 acknowledgment = (ORR_O02) response;
            String acknowledgmentCode = acknowledgment.getMSA().getAcknowledgmentCode().getValue();

            logger.info("Received ORR_O02 acknowledgment: {}", acknowledgmentCode);
            processAcknowledgement(modality, responseMessage, acknowledgmentCode);

        } else if (response instanceof ACK) {
            ACK acknowledgment = (ACK) response;
            String acknowledgmentCode = acknowledgment.getMSA().getAcknowledgmentCode().getValue();

            logger.info("Received ACK acknowledgment: {}", acknowledgmentCode);
            processAcknowledgement(modality, responseMessage, acknowledgmentCode);

        } else {
            logger.error("Unsupported HL7 response type: {}", response.getClass().getSimpleName());
            throw new ModalityException(responseMessage, modality);
        }

        logger.info("HL7 message successfully processed for orderType: {}", orderType);
        return responseMessage;
    }

    Message post(Modality modality, Message requestMessage)
            throws LLPException, IOException, HL7Exception {

        Connection connection = null;
        try {
            logger.info("Opening HL7 connection to {}:{}",
                    modality.getIp(), modality.getPort());

            HapiContext hapiContext = new DefaultHapiContext();
            connection = hapiContext.newClient(modality.getIp(), modality.getPort(), false);

            Initiator initiator = connection.getInitiator();
            logger.debug("Sending HL7 message...");
            return initiator.sendAndReceive(requestMessage);

        } catch (Exception e) {
            logger.error("Error while sending HL7 message to modality", e);
            throw e;
        } finally {
            if (connection != null) {
                connection.close();
                logger.debug("HL7 connection closed");
            }
        }
    }

    String parseResponse(Message response) throws HL7Exception {
        logger.debug("Parsing HL7 response");
        return new PipeParser().encode(response);
    }

    private void processAcknowledgement(Modality modality,
                                        String responseMessage,
                                        String acknowledgmentCode) {

        if (!AcknowledgmentCode.AA.toString().equals(acknowledgmentCode)) {
            logger.error("Negative acknowledgment from modality {}: {}",
                    modality.getName(), acknowledgmentCode);
            throw new ModalityException(responseMessage, modality);
        }

        logger.info("Acknowledgment successful from modality: {}", modality.getName());
    }
}
