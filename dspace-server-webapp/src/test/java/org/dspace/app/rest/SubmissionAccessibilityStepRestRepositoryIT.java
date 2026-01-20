/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.eperson.EPerson;
import org.dspace.core.Context;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for Accessibility Step in the Submission workflow.
 *
 * Tests the REST API endpoints for the accessibility acknowledgment step,
 * including retrieving step data and processing PATCH operations.
 */


public class SubmissionAccessibilityStepRestRepositoryIT extends AbstractControllerIntegrationTest {

    @BeforeClass
    public static void setupMetadataFields() throws Exception {
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try {
            MetadataSchemaService schemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
            MetadataFieldService fieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

            // Get local schema
            MetadataSchema localSchema = schemaService.findByNamespace(context, "http://dspace.org/local/");
            if (localSchema == null) {
                localSchema = schemaService.create(context, "local", "http://dspace.org/local/");
            }

            // Create fields if they don't exist
            if (fieldService.findByElement(context, localSchema, "accessibility", "acknowledged") == null) {
                fieldService.create(context, localSchema, "accessibility", "acknowledged",
                        "Accessibility guidelines acknowledgment status");
            }

            if (fieldService.findByElement(context, localSchema, "accessibility", "acknowledgedDate") == null) {
                fieldService.create(context, localSchema, "accessibility", "acknowledgedDate",
                        "Date when accessibility guidelines were acknowledged");
            }

            context.complete();
        } finally {
            if (context.isValid()) {
                context.abort();
            }
        }
    }
    @Autowired
    private ItemService itemService;

    @Test
    public void testGetAccessibilityStep_NotAcknowledged() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create a submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        // Create a collection
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        context.restoreAuthSystemState();

        // Create a workspace item
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withSubmitter(submitter)
                .build();

        String authToken = getAuthToken(submitter.getEmail(), password);

        // Get the accessibility step data
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.accessibility.granted", is(false)))
                .andExpect(jsonPath("$.sections.accessibility.acceptanceDate", nullValue()));
    }

    @Test
    public void testPatchAccessibilityStep_GrantAcknowledgment() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        context.restoreAuthSystemState();

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withSubmitter(submitter)
                .build();

        String authToken = getAuthToken(submitter.getEmail(), password);

        // Patch to grant acknowledgment
        List<Operation> ops = new ArrayList<>();
        ops.add(new AddOperation("/sections/accessibility/granted", true));

        String patchBody = getPatchContent(ops);

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType("application/json-patch+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.accessibility.granted", is(true)))
                .andExpect(jsonPath("$.sections.accessibility.acceptanceDate", notNullValue()));

        // Verify metadata was saved
        witem = context.reloadEntity(witem);
        String metadata = itemService.getMetadataFirstValue(
                witem.getItem(), "local", "accessibility", "acknowledged", null);
        org.junit.Assert.assertEquals("Metadata should be 'true'", "true", metadata);
    }

    @Test
    public void testAccessibilityValidation_BlocksSubmissionWhenNotAcknowledged() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        context.restoreAuthSystemState();

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withSubmitter(submitter)
                .withTitle("Test Item")
                .withIssueDate("2025-01-15")
                .build();

        String authToken = getAuthToken(submitter.getEmail(), password);

        // Try to complete submission without acknowledging
        // This should return validation errors
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[?(@.message=='error.validation.accessibility.required')]",
                        notNullValue()));
    }

    @Test
    public void testAccessibilityValidation_AllowsSubmissionWhenAcknowledged() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        context.restoreAuthSystemState();

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withSubmitter(submitter)
                .withTitle("Test Item")
                .withIssueDate("2025-01-15")
                .build();

        String authToken = getAuthToken(submitter.getEmail(), password);

        // Grant acknowledgment
        List<Operation> ops = new ArrayList<>();
        ops.add(new AddOperation("/sections/accessibility/granted", true));
        String patchBody = getPatchContent(ops);

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType("application/json-patch+json"))
                .andExpect(status().isOk());

        // Now validation should pass - no accessibility errors
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.accessibility.granted", is(true)))
                .andExpect(jsonPath("$.errors[?(@.message=='error.validation.accessibility.required')]")
                        .doesNotExist());
    }

    @Test
    public void testGetAccessibilityStep_PreservesPreviousAcknowledgment() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        // Create workspace item with pre-existing acknowledgment metadata
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withSubmitter(submitter)
                .build();

        // Add acknowledgment metadata
        itemService.addMetadata(context, witem.getItem(), "local", "accessibility",
                "acknowledged", null, "true");
        itemService.addMetadata(context, witem.getItem(), "local", "accessibility",
                "acknowledgedDate", null, "2025-01-15T10:30:00Z");
        itemService.update(context, witem.getItem());

        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);

        // Get the workspace item - should show previous acknowledgment
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.accessibility.granted", is(true)))
                .andExpect(jsonPath("$.sections.accessibility.acceptanceDate",
                        is("2025-01-15T10:30:00Z")));
    }

    @Test
    public void testPatchAccessibilityStep_Unauthorized() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        EPerson otherUser = EPersonBuilder.createEPerson(context)
                .withEmail("other@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        context.restoreAuthSystemState();

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withSubmitter(submitter)
                .build();

        // Try to patch with different user's token
        String authToken = getAuthToken(otherUser.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ops.add(new AddOperation("/sections/accessibility/granted", true));
        String patchBody = getPatchContent(ops);

        getClient(authToken).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(patchBody)
                        .contentType("application/json-patch+json"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetAccessibilityStep_Anonymous() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withSubmitter(submitter)
                .build();

        context.restoreAuthSystemState();

        // Try to access without authentication
        getClient().perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testAccessibilityStep_CaseInsensitiveMetadataValue() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        // Create workspace item with uppercase "TRUE"
        WorkspaceItem witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withSubmitter(submitter)
                .build();

        itemService.addMetadata(context, witem.getItem(), "local", "accessibility",
                "acknowledged", null, "TRUE");
        itemService.update(context, witem.getItem());

        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);

        // Should recognize uppercase TRUE as acknowledged
        getClient(authToken).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.accessibility.granted", is(true)));
    }
}