/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.submit.step.validation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validation for the accessibility acknowledgment step.
 * Checks if the user has acknowledged the guidelines by looking at item metadata.
 */
public class AccessibilityValidation extends AbstractValidation {

    private static final Logger log = LoggerFactory.getLogger(AccessibilityValidation.class);

    public static final String ERROR_VALIDATION_REQUIRED = "error.validation.accessibility.required";

    @Autowired
    private ItemService itemService;

    @Override
    public List<ErrorRest> validate(SubmissionService submissionService,
                                    InProgressSubmission obj,
                                    SubmissionStepConfig config) throws SQLException {

        List<ErrorRest> errors = new ArrayList<>();

        log.info("=== ACCESSIBILITY VALIDATION ===");
        log.info("Validating submission: {}", obj.getID());

        // Check if accessibility was granted by looking at item metadata
        Item item = obj.getItem();
        String ackMetadata = "local.accessibility.acknowledged";
        List<MetadataValue> acknowledgedValues = itemService.getMetadataByMetadataString(item, ackMetadata);

        boolean isGranted = false;
        if (!acknowledgedValues.isEmpty()) {
            String value = acknowledgedValues.get(0).getValue();
            isGranted = "true".equalsIgnoreCase(value);
        }

        log.info("Accessibility granted in metadata: {}", isGranted);

        // If not granted, add validation error
        if (!isGranted) {
            log.warn("Accessibility not acknowledged - blocking submission");
            addError(errors, ERROR_VALIDATION_REQUIRED, "/sections/accessibility/");
        } else {
            log.info("Accessibility acknowledged - validation passed");
        }

        log.info("=== END ACCESSIBILITY VALIDATION - Errors: {} ===", errors.size());

        return errors;
    }
}