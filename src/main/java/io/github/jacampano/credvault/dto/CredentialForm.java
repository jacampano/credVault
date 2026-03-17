package io.github.jacampano.credvault.dto;

import io.github.jacampano.credvault.domain.CredentialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public class CredentialForm {

    @NotBlank
    @Size(max = 120)
    private String identifier;

    @NotBlank
    @Size(max = 120)
    private String createdBy;

    @NotNull
    private Long componentId;

    @NotNull
    private Long environmentId;

    private String systemName;

    @NotNull
    private CredentialType type = CredentialType.WEB_USER_PASSWORD;

    private Set<String> selectedGroups = new LinkedHashSet<>();

    private boolean shared;

    @Size(max = 255)
    private String notes;

    @Size(max = 255)
    private String webUsername;

    @Size(max = 255)
    private String webPassword;

    @Size(max = 2048)
    private String webUrl;

    @Size(max = 4096)
    private String tokenValue;

    @Size(max = 2048)
    private String tokenUrl;

    private LocalDate tokenExpirationDate;

    private boolean tokenNoExpiry;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getComponentId() {
        return componentId;
    }

    public void setComponentId(Long componentId) {
        this.componentId = componentId;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public CredentialType getType() {
        return type;
    }

    public void setType(CredentialType type) {
        this.type = type;
    }

    public Set<String> getSelectedGroups() {
        return selectedGroups;
    }

    public void setSelectedGroups(Set<String> selectedGroups) {
        this.selectedGroups = selectedGroups == null ? new LinkedHashSet<>() : selectedGroups;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getWebUsername() {
        return webUsername;
    }

    public void setWebUsername(String webUsername) {
        this.webUsername = webUsername;
    }

    public String getWebPassword() {
        return webPassword;
    }

    public void setWebPassword(String webPassword) {
        this.webPassword = webPassword;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public LocalDate getTokenExpirationDate() {
        return tokenExpirationDate;
    }

    public void setTokenExpirationDate(LocalDate tokenExpirationDate) {
        this.tokenExpirationDate = tokenExpirationDate;
    }

    public boolean isTokenNoExpiry() {
        return tokenNoExpiry;
    }

    public void setTokenNoExpiry(boolean tokenNoExpiry) {
        this.tokenNoExpiry = tokenNoExpiry;
    }
}
