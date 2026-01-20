/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accessibility step for DSpace Spring Rest.
 * Requires users to acknowledge they have read accessibility guidelines.
 *
 * Stores the acknowledgment  as item metadata (local.accessibility.acknowledged = true)
 * and the date of acknowledgment (local.accessibility.acknowledgedDate).
 */
public class AccessibilityStep extends AbstractProcessingStep {

    private static final Logger log = LoggerFactory.getLogger(AccessibilityStep.class);

    // Metadata fields for storing acknowledgment
    private static final String METADATA_SCHEMA = "local";
    private static final String METADATA_ELEMENT = "accessibility";
    private static final String METADATA_QUALIFIER_ACK = "acknowledged";
    private static final String METADATA_QUALIFIER_DATE = "acknowledgedDate";

    @Override
    public <T extends Serializable> T getData(SubmissionService submissionService, InProgressSubmission obj,
                                              SubmissionStepConfig config) throws Exception {

        DataAccessibility result = new DataAccessibility();
        Item item = obj.getItem();

        // Check if accessibility was already granted
        String ackMetadata = METADATA_SCHEMA + "." + METADATA_ELEMENT + "." + METADATA_QUALIFIER_ACK;
        List<MetadataValue> acknowledgedValues = itemService.getMetadataByMetadataString(item, ackMetadata);

        if (!acknowledgedValues.isEmpty() && "true".equalsIgnoreCase(acknowledgedValues.get(0).getValue())) {
            result.setGranted(true);

            // Get acknowledgment date if available
            String dateMetadata = METADATA_SCHEMA + "." + METADATA_ELEMENT + "." + METADATA_QUALIFIER_DATE;
            List<MetadataValue> dateValues = itemService.getMetadataByMetadataString(item, dateMetadata);
            if (!dateValues.isEmpty()) {
                result.setAcceptanceDate(dateValues.get(0).getValue());
            }

            log.debug("Accessibility already granted");
        }

        log.debug("AccessibilityStep getData - granted: {}", result.isGranted());

        return (T) result;
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
                                  Operation op, SubmissionStepConfig stepConf) throws Exception {

        log.debug("=== ACCESSIBILITY STEP PATCH ===");
        log.debug("Operation: {}", op.getOp());
        log.debug("Path: {}", op.getPath());
        log.debug("Value: {}", op.getValue());

        String path = op.getPath();

        // Check if this is the "granted" operation
        if (path.endsWith(ACCESSIBILITY_STEP_OPERATION_ENTRY)) {
            log.info("Processing accessibility acknowledgment");

            // Get the PatchOperation handler
            PatchOperation<String> patchOperation = new PatchOperationFactory()
                    .instanceOf(ACCESSIBILITY_STEP_OPERATION_ENTRY, op.getOp());

            // Execute the patch operation
            patchOperation.perform(context, currentRequest, source, op);

            // Store acknowledgment as item metadata
            Item item = source.getItem();

            // Add/update the acknowledgment metadata
            itemService.setMetadataSingleValue(context, item, METADATA_SCHEMA, METADATA_ELEMENT,
                    METADATA_QUALIFIER_ACK, null, "true");

            // Add the acknowledgment date
            String dateStr = new Date().toString();
            itemService.setMetadataSingleValue(context, item, METADATA_SCHEMA, METADATA_ELEMENT,
                    METADATA_QUALIFIER_DATE, null, dateStr);

            itemService.update(context, item);

            log.info("Accessibility acknowledgment stored as metadata");

        } else {
            throw new UnprocessableEntityException("The path " + op.getPath() + " cannot be patched");
        }

        log.debug("=== END ACCESSIBILITY STEP PATCH ===");
    }

    /**
     * Inner class to hold accessibility data
     */
    public static class DataAccessibility implements Serializable {
        private static final long serialVersionUID = 1L;

        private boolean granted = false;
        private String acceptanceDate;

        public boolean isGranted() {
            return granted;
        }

        public void setGranted(boolean granted) {
            this.granted = granted;
        }

        public String getAcceptanceDate() {
            return acceptanceDate;
        }

        public void setAcceptanceDate(String acceptanceDate) {
            this.acceptanceDate = acceptanceDate;
        }
    }
}