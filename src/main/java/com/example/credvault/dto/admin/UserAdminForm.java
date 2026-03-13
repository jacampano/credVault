package com.example.credvault.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;

public class UserAdminForm {

    @NotBlank
    @Size(max = 120)
    private String username;

    @Size(max = 120)
    private String firstName;

    @Size(max = 120)
    private String lastName;

    @Email
    @Size(max = 200)
    private String email;

    private boolean enabled;
    private boolean appUserRole;
    private boolean adminRole;

    private Set<String> selectedTeams = new LinkedHashSet<>();

    @Size(max = 120)
    private String newPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAppUserRole() {
        return appUserRole;
    }

    public void setAppUserRole(boolean appUserRole) {
        this.appUserRole = appUserRole;
    }

    public boolean isAdminRole() {
        return adminRole;
    }

    public void setAdminRole(boolean adminRole) {
        this.adminRole = adminRole;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public Set<String> getSelectedTeams() {
        return selectedTeams;
    }

    public void setSelectedTeams(Set<String> selectedTeams) {
        this.selectedTeams = selectedTeams == null ? new LinkedHashSet<>() : selectedTeams;
    }
}
