package com.example.credvault.logging;

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
    void classifyAdminUsersUpdateRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("POST", "/admin/users/12");
        assertThat(route.section()).isEqualTo("admin/users");
        assertThat(route.action()).isEqualTo("update_user");
    }

    @Test
    void classifyAdminTeamsDeleteRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("POST", "/admin/teams/9/delete");
        assertThat(route.section()).isEqualTo("admin/teams");
        assertThat(route.action()).isEqualTo("delete_team");
    }

    @Test
    void classifyFallbackRoute() {
        AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify("GET", "/unknown/path");
        assertThat(route.section()).isEqualTo("other");
        assertThat(route.action()).isEqualTo("access_resource");
    }
}
