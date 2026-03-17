package io.github.jacampano.credvault.domain;

import io.github.jacampano.credvault.crypto.EncryptedStringConverter;
import io.github.jacampano.credvault.domain.catalog.AppEnvironment;
import io.github.jacampano.credvault.domain.catalog.InformationComponent;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "credentials")
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String identifier;

    @Column(nullable = false, length = 120)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "information_component_id", nullable = false)
    private InformationComponent informationComponent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_environment_id", nullable = false)
    private AppEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CredentialType type = CredentialType.WEB_USER_PASSWORD;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "credential_groups", joinColumns = @JoinColumn(name = "credential_id"))
    @Column(name = "group_name", nullable = false, length = 120)
    private Set<String> groups = new HashSet<>();

    @Column(nullable = false)
    private boolean shared;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_web_username", length = 4096)
    private String webUsername;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_web_password", length = 4096)
    private String webPassword;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_web_url", length = 4096)
    private String webUrl;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_token_value", length = 4096)
    private String tokenValue;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_token_url", length = 4096)
    private String tokenUrl;

    @Column(name = "token_expiration_date")
    private LocalDate tokenExpirationDate;

    @Column(name = "token_no_expiry", nullable = false)
    private boolean tokenNoExpiry;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "secret_notes", length = 4096)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    @Column
    private Instant deletedAt;

    @Column(length = 120)
    private String deletedBy;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public InformationComponent getInformationComponent() {
        return informationComponent;
    }

    public void setInformationComponent(InformationComponent informationComponent) {
        this.informationComponent = informationComponent;
    }

    public AppEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(AppEnvironment environment) {
        this.environment = environment;
    }

    public CredentialType getType() {
        return type;
    }

    public void setType(CredentialType type) {
        this.type = type;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }
}
