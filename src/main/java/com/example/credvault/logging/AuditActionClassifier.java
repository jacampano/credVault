package com.example.credvault.logging;

public final class AuditActionClassifier {

    private AuditActionClassifier() {
    }

    public static AuditRoute classify(String method, String path) {
        String normalizedMethod = method == null ? "UNKNOWN" : method.toUpperCase();
        String normalizedPath = normalizePath(path);

        if (normalizedPath.startsWith("/credentials")) {
            return classifyCredentials(normalizedMethod, normalizedPath);
        }
        if (normalizedPath.startsWith("/admin/teams")) {
            return classifyAdminTeams(normalizedMethod, normalizedPath);
        }
        if (normalizedPath.startsWith("/admin/users")) {
            return classifyAdminUsers(normalizedMethod, normalizedPath);
        }
        if (normalizedPath.startsWith("/admin/authentication")) {
            return new AuditRoute("admin/authentication",
                    "POST".equals(normalizedMethod) ? "save_authentication_settings" : "view_authentication_settings");
        }
        if ("/admin".equals(normalizedPath)) {
            return new AuditRoute("admin", "view_admin_home");
        }
        if ("/profile".equals(normalizedPath)) {
            return new AuditRoute("profile", "POST".equals(normalizedMethod) ? "update_profile" : "view_profile");
        }
        if ("/login".equals(normalizedPath)) {
            return new AuditRoute("auth", "open_login");
        }
        if ("/logout".equals(normalizedPath)) {
            return new AuditRoute("auth", "logout");
        }
        if ("/".equals(normalizedPath)) {
            return new AuditRoute("home", "open_home");
        }
        return new AuditRoute("other", "access_resource");
    }

    private static AuditRoute classifyCredentials(String method, String path) {
        if ("GET".equals(method) && "/credentials".equals(path)) {
            return new AuditRoute("credentials", "list_credentials");
        }
        if ("GET".equals(method) && "/credentials/new".equals(path)) {
            return new AuditRoute("credentials", "open_create_credential");
        }
        if ("POST".equals(method) && "/credentials".equals(path)) {
            return new AuditRoute("credentials", "create_credential");
        }
        if ("GET".equals(method) && path.matches("^/credentials/\\d+/edit$")) {
            return new AuditRoute("credentials", "open_edit_credential");
        }
        if ("POST".equals(method) && path.matches("^/credentials/\\d+$")) {
            return new AuditRoute("credentials", "update_credential");
        }
        if ("POST".equals(method) && path.matches("^/credentials/\\d+/delete$")) {
            return new AuditRoute("credentials", "delete_credential");
        }
        if ("GET".equals(method) && path.matches("^/credentials/\\d+/history$")) {
            return new AuditRoute("credentials", "view_credential_history");
        }
        return new AuditRoute("credentials", "access_credentials");
    }

    private static AuditRoute classifyAdminUsers(String method, String path) {
        if ("GET".equals(method) && "/admin/users".equals(path)) {
            return new AuditRoute("admin/users", "list_users");
        }
        if ("GET".equals(method) && "/admin/users/new".equals(path)) {
            return new AuditRoute("admin/users", "open_create_user");
        }
        if ("POST".equals(method) && "/admin/users".equals(path)) {
            return new AuditRoute("admin/users", "create_user");
        }
        if ("GET".equals(method) && path.matches("^/admin/users/\\d+/edit$")) {
            return new AuditRoute("admin/users", "open_edit_user");
        }
        if ("POST".equals(method) && path.matches("^/admin/users/\\d+$")) {
            return new AuditRoute("admin/users", "update_user");
        }
        if ("POST".equals(method) && path.matches("^/admin/users/\\d+/toggle-status$")) {
            return new AuditRoute("admin/users", "toggle_user_status");
        }
        if ("POST".equals(method) && path.matches("^/admin/users/\\d+/delete$")) {
            return new AuditRoute("admin/users", "delete_user");
        }
        return new AuditRoute("admin/users", "access_user_management");
    }

    private static AuditRoute classifyAdminTeams(String method, String path) {
        if ("GET".equals(method) && "/admin/teams".equals(path)) {
            return new AuditRoute("admin/teams", "list_teams");
        }
        if ("GET".equals(method) && "/admin/teams/new".equals(path)) {
            return new AuditRoute("admin/teams", "open_create_team");
        }
        if ("POST".equals(method) && "/admin/teams".equals(path)) {
            return new AuditRoute("admin/teams", "create_team");
        }
        if ("GET".equals(method) && path.matches("^/admin/teams/\\d+/edit$")) {
            return new AuditRoute("admin/teams", "open_edit_team");
        }
        if ("POST".equals(method) && path.matches("^/admin/teams/\\d+$")) {
            return new AuditRoute("admin/teams", "update_team");
        }
        if ("POST".equals(method) && path.matches("^/admin/teams/\\d+/delete$")) {
            return new AuditRoute("admin/teams", "delete_team");
        }
        return new AuditRoute("admin/teams", "access_team_management");
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String clean = path.trim();
        int queryIndex = clean.indexOf('?');
        if (queryIndex >= 0) {
            clean = clean.substring(0, queryIndex);
        }
        if (clean.length() > 1 && clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean.isBlank() ? "/" : clean;
    }

    public record AuditRoute(String section, String action) {
    }
}
