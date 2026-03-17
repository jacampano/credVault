package io.github.jacampano.credvault.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditActionClassifierTest {

    @Test
    void classifyCredentialsHistoryRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("GET", "/credentials/25/history");
        assertThat(route.section()).isEqualTo("credentials");
        assertThat(route.action()).isEqualTo("view_credential_history");
    }

    @Test
    void classifyCredentialsCopyRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("POST", "/credentials/25/copy");
        assertThat(route.section()).isEqualTo("credentials");
        assertThat(route.action()).isEqualTo("copy_credential_value");
    }

    @Test
    void classifyAdminUsersUpdateRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("POST", "/admin/users/12");
        assertThat(route.section()).isEqualTo("admin/users");
        assertThat(route.action()).isEqualTo("update_user");
    }

    @Test
    void classifyAdminGroupsDeleteRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("POST", "/admin/groups/9/delete");
        assertThat(route.section()).isEqualTo("admin/groups");
        assertThat(route.action()).isEqualTo("delete_group");
    }

    @Test
    void classifyAdminTrashRestoreRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("POST", "/admin/trash/14/restore");
        assertThat(route.section()).isEqualTo("admin/trash");
        assertThat(route.action()).isEqualTo("restore_credential");
    }

    @Test
    void classifyAdminInformationSystemsCreateRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("POST", "/admin/information-systems");
        assertThat(route.section()).isEqualTo("admin/information-systems");
        assertThat(route.action()).isEqualTo("create_information_system");
    }

    @Test
    void classifyAdminComponentsEditRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("GET", "/admin/components/8/edit");
        assertThat(route.section()).isEqualTo("admin/components");
        assertThat(route.action()).isEqualTo("open_edit_component");
    }

    @Test
    void classifyAdminEnvironmentsCreateRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("POST", "/admin/environments");
        assertThat(route.section()).isEqualTo("admin/environments");
        assertThat(route.action()).isEqualTo("create_environment");
    }

    @Test
    void classifyCalendarViewRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("GET", "/calendar");
        assertThat(route.section()).isEqualTo("calendar");
        assertThat(route.action()).isEqualTo("view_calendar");
    }

    @Test
    void classifyFallbackRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("GET", "/unknown/path");
        assertThat(route.section()).isEqualTo("other");
        assertThat(route.action()).isEqualTo("access_resource");
    }
}
