package io.github.jacampano.credvault.logging;

public final class AuditActionClassifier {

    private AuditActionClassifier() {
    }

    public static AuditRoute classify(String method, String path) {
        String normalizedMethod = method == null ? "UNKNOWN" : method.toUpperCase();
        String normalizedPath = normalizePath(path);

        if (normalizedPath.startsWith("/credentials")) {
            return classifyCredentials(normalizedMethod, normalizedPath);
        }
        if (normalizedPath.startsWith("/admin/groups")) {
            return classifyAdminGroups(normalizedMethod, normalizedPath);
        }
        if (normalizedPath.startsWith("/admin/users")) {
            return classifyAdminUsers(normalizedMethod, normalizedPath);
        }
        if (normalizedPath.startsWith("/admin/information-systems")) {
            return classifyAdminInformationSystems(normalizedMethod, normalizedPath);
        }
        if (normalizedPath.startsWith("/admin/components")) {
            return classifyAdminComponents(normalizedMethod, normalizedPath);
        }
        if (normalizedPath.startsWith("/admin/environments")) {
            return classifyAdminEnvironments(normalizedMethod, normalizedPath);
        }
        if (normalizedPath.startsWith("/admin/trash")) {
            return classifyAdminTrash(normalizedMethod, normalizedPath);
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
        if ("/calendar".equals(normalizedPath)) {
            return new AuditRoute("calendar", "view_calendar");
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
        if ("POST".equals(method) && path.matches("^/credentials/\\d+/copy$")) {
            return new AuditRoute("credentials", "copy_credential_value");
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

    private static AuditRoute classifyAdminGroups(String method, String path) {
        if ("GET".equals(method) && "/admin/groups".equals(path)) {
            return new AuditRoute("admin/groups", "list_groups");
        }
        if ("GET".equals(method) && "/admin/groups/new".equals(path)) {
            return new AuditRoute("admin/groups", "open_create_group");
        }
        if ("POST".equals(method) && "/admin/groups".equals(path)) {
            return new AuditRoute("admin/groups", "create_group");
        }
        if ("GET".equals(method) && path.matches("^/admin/groups/\\d+/edit$")) {
            return new AuditRoute("admin/groups", "open_edit_group");
        }
        if ("POST".equals(method) && path.matches("^/admin/groups/\\d+$")) {
            return new AuditRoute("admin/groups", "update_group");
        }
        if ("POST".equals(method) && path.matches("^/admin/groups/\\d+/delete$")) {
            return new AuditRoute("admin/groups", "delete_group");
        }
        return new AuditRoute("admin/groups", "access_group_management");
    }

    private static AuditRoute classifyAdminTrash(String method, String path) {
        if ("GET".equals(method) && "/admin/trash".equals(path)) {
            return new AuditRoute("admin/trash", "view_trash");
        }
        if ("POST".equals(method) && path.matches("^/admin/trash/\\d+/restore$")) {
            return new AuditRoute("admin/trash", "restore_credential");
        }
        if ("POST".equals(method) && path.matches("^/admin/trash/\\d+/delete-permanently$")) {
            return new AuditRoute("admin/trash", "delete_credential_permanently");
        }
        return new AuditRoute("admin/trash", "access_trash");
    }

    private static AuditRoute classifyAdminInformationSystems(String method, String path) {
        if ("GET".equals(method) && "/admin/information-systems".equals(path)) {
            return new AuditRoute("admin/information-systems", "list_information_systems");
        }
        if ("GET".equals(method) && "/admin/information-systems/new".equals(path)) {
            return new AuditRoute("admin/information-systems", "open_create_information_system");
        }
        if ("POST".equals(method) && "/admin/information-systems".equals(path)) {
            return new AuditRoute("admin/information-systems", "create_information_system");
        }
        if ("GET".equals(method) && path.matches("^/admin/information-systems/\\d+/edit$")) {
            return new AuditRoute("admin/information-systems", "open_edit_information_system");
        }
        if ("POST".equals(method) && path.matches("^/admin/information-systems/\\d+$")) {
            return new AuditRoute("admin/information-systems", "update_information_system");
        }
        if ("POST".equals(method) && path.matches("^/admin/information-systems/\\d+/delete$")) {
            return new AuditRoute("admin/information-systems", "delete_information_system");
        }
        return new AuditRoute("admin/information-systems", "access_information_system_management");
    }

    private static AuditRoute classifyAdminComponents(String method, String path) {
        if ("GET".equals(method) && "/admin/components".equals(path)) {
            return new AuditRoute("admin/components", "list_components");
        }
        if ("GET".equals(method) && "/admin/components/new".equals(path)) {
            return new AuditRoute("admin/components", "open_create_component");
        }
        if ("POST".equals(method) && "/admin/components".equals(path)) {
            return new AuditRoute("admin/components", "create_component");
        }
        if ("GET".equals(method) && path.matches("^/admin/components/\\d+/edit$")) {
            return new AuditRoute("admin/components", "open_edit_component");
        }
        if ("POST".equals(method) && path.matches("^/admin/components/\\d+$")) {
            return new AuditRoute("admin/components", "update_component");
        }
        if ("POST".equals(method) && path.matches("^/admin/components/\\d+/delete$")) {
            return new AuditRoute("admin/components", "delete_component");
        }
        return new AuditRoute("admin/components", "access_component_management");
    }

    private static AuditRoute classifyAdminEnvironments(String method, String path) {
        if ("GET".equals(method) && "/admin/environments".equals(path)) {
            return new AuditRoute("admin/environments", "list_environments");
        }
        if ("GET".equals(method) && "/admin/environments/new".equals(path)) {
            return new AuditRoute("admin/environments", "open_create_environment");
        }
        if ("POST".equals(method) && "/admin/environments".equals(path)) {
            return new AuditRoute("admin/environments", "create_environment");
        }
        if ("GET".equals(method) && path.matches("^/admin/environments/\\d+/edit$")) {
            return new AuditRoute("admin/environments", "open_edit_environment");
        }
        if ("POST".equals(method) && path.matches("^/admin/environments/\\d+$")) {
            return new AuditRoute("admin/environments", "update_environment");
        }
        if ("POST".equals(method) && path.matches("^/admin/environments/\\d+/delete$")) {
            return new AuditRoute("admin/environments", "delete_environment");
        }
        return new AuditRoute("admin/environments", "access_environment_management");
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
