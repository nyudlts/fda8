/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for Accessibility Step metadata handling.
 *
 * Tests the metadata storage and retrieval for accessibility acknowledgment.
 */
public class SubmissionAccessibilityStepRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    /**
     * Register accessibility metadata fields for testing.
     */
    @BeforeClass
    public static void setupMetadataFields() throws Exception {
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try {
            MetadataSchemaService schemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
            MetadataFieldService fieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

            // Get 'local' schema - it should already exist
            MetadataSchema localSchema = schemaService.findByNamespace(context, "http://dspace.org/local/");
            if (localSchema == null) {
                localSchema = schemaService.find(context, "local");
            }

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
        } catch (Exception e) {
            if (context.isValid()) {
                context.abort();
            }
            throw e;
        }
    }

    @Test
    public void testAccessibilityMetadata_StorageAndRetrieval() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson submitter = EPersonBuilder.createEPerson(context)
                .withEmail("submitter@example.com")
                .withPassword(password)
                .build();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, community)
                .withName("Collection 1")
                .withSubmitterGroup(submitter)
                .build();

        // Create workspace item and get its item
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withSubmitter(submitter)
                .build();

        Item item = workspaceItem.getItem();

        // Add accessibility metadata
        itemService.addMetadata(context, item, "local", "accessibility", "acknowledged", null, "true");
        itemService.addMetadata(context, item, "local", "accessibility", "acknowledgedDate", null, "2025-01-19");
        itemService.update(context, item);

        context.restoreAuthSystemState();
        context.commit();

        // Verify metadata was stored
        Item retrievedItem = itemService.find(context, item.getID());
        String ackValue = itemService.getMetadataFirstValue(retrievedItem, "local", "accessibility",
                "acknowledged", null);
        String dateValue = itemService.getMetadataFirstValue(retrievedItem, "local", "accessibility",
                "acknowledgedDate", null);

        org.junit.Assert.assertEquals("Acknowledgment should be 'true'", "true", ackValue);
        org.junit.Assert.assertEquals("Date should match", "2025-01-19", dateValue);
    }

    @Test
    public void testAccessibilityMetadata_CaseInsensitive() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, community)
                .withName("Collection 1")
                .build();

        // Create workspace item and get its item
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .build();

        Item item = workspaceItem.getItem();

        // Add uppercase "TRUE"
        itemService.addMetadata(context, item, "local", "accessibility", "acknowledged", null, "TRUE");
        itemService.update(context, item);

        context.restoreAuthSystemState();
        context.commit();

        // Verify we can retrieve and check case-insensitively
        Item retrievedItem = itemService.find(context, item.getID());
        String ackValue = itemService.getMetadataFirstValue(retrievedItem, "local", "accessibility",
                "acknowledged", null);

        org.junit.Assert.assertNotNull("Metadata should exist", ackValue);
        org.junit.Assert.assertTrue("Should handle case-insensitively",
                "true".equalsIgnoreCase(ackValue));
    }

    @Test
    public void testAccessibilityMetadata_MultipleSets() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, community)
                .withName("Collection 1")
                .build();

        // Create workspace item and get its item
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .build();

        Item item = workspaceItem.getItem();

        // First set
        itemService.addMetadata(context, item, "local", "accessibility", "acknowledged", null, "false");
        itemService.update(context, item);

        // Update to true
        itemService.clearMetadata(context, item, "local", "accessibility", "acknowledged", null);
        itemService.addMetadata(context, item, "local", "accessibility", "acknowledged", null, "true");
        itemService.addMetadata(context, item, "local", "accessibility", "acknowledgedDate", null, "2025-01-19");
        itemService.update(context, item);

        context.restoreAuthSystemState();
        context.commit();

        // Verify final state
        Item retrievedItem = itemService.find(context, item.getID());
        String ackValue = itemService.getMetadataFirstValue(retrievedItem, "local", "accessibility",
                "acknowledged", null);
        String dateValue = itemService.getMetadataFirstValue(retrievedItem, "local", "accessibility",
                "acknowledgedDate", null);

        org.junit.Assert.assertEquals("Final value should be 'true'", "true", ackValue);
        org.junit.Assert.assertNotNull("Date should be set", dateValue);
    }

    @Test
    public void testAccessibilityMetadata_ClearMetadata() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, community)
                .withName("Collection 1")
                .build();

        // Create workspace item and get its item
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .build();

        Item item = workspaceItem.getItem();

        // Add metadata
        itemService.addMetadata(context, item, "local", "accessibility", "acknowledged", null, "true");
        itemService.addMetadata(context, item, "local", "accessibility", "acknowledgedDate", null, "2025-01-19");
        itemService.update(context, item);

        // Clear metadata
        itemService.clearMetadata(context, item, "local", "accessibility", "acknowledged", null);
        itemService.clearMetadata(context, item, "local", "accessibility", "acknowledgedDate", null);
        itemService.update(context, item);

        context.restoreAuthSystemState();
        context.commit();

        // Verify metadata was cleared
        Item retrievedItem = itemService.find(context, item.getID());
        String ackValue = itemService.getMetadataFirstValue(retrievedItem, "local", "accessibility",
                "acknowledged", null);
        String dateValue = itemService.getMetadataFirstValue(retrievedItem, "local", "accessibility",
                "acknowledgedDate", null);

        org.junit.Assert.assertNull("Acknowledgment should be cleared", ackValue);
        org.junit.Assert.assertNull("Date should be cleared", dateValue);
    }
}