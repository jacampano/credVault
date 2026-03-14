package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.repository.CredentialHistoryRepository;
import io.github.jacampano.credvault.repository.CredentialRepository;
import io.github.jacampano.credvault.repository.catalog.AppEnvironmentRepository;
import io.github.jacampano.credvault.repository.catalog.InformationComponentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private CredentialHistoryRepository credentialHistoryRepository;

    @Mock
    private InformationComponentRepository informationComponentRepository;

    @Mock
    private AppEnvironmentRepository appEnvironmentRepository;

    private CredentialService credentialService;

    @BeforeEach
    void setUp() {
        credentialService = new CredentialService(
                credentialRepository,
                credentialHistoryRepository,
                informationComponentRepository,
                appEnvironmentRepository,
                List.of()
        );
    }

    @Test
    void deleteMovesCredentialToTrash() {
        Credential credential = new Credential();
        credential.setId(5L);
        credential.setCreatedBy("ana");
        when(credentialRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(credential));

        credentialService.delete(5L, "admin");

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void restoreRemovesDeletedMarks() {
        Credential credential = new Credential();
        credential.setId(6L);
        credential.setDeleted(true);
        credential.setDeletedBy("ana");
        when(credentialRepository.findByIdAndDeletedTrue(6L)).thenReturn(Optional.of(credential));

        credentialService.restore(6L);

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isFalse();
        assertThat(captor.getValue().getDeletedBy()).isNull();
        assertThat(captor.getValue().getDeletedAt()).isNull();
    }

    @Test
    void deletePermanentlyDeletesHistoryThenCredential() {
        Credential credential = new Credential();
        credential.setId(7L);
        credential.setDeleted(true);
        when(credentialRepository.findByIdAndDeletedTrue(7L)).thenReturn(Optional.of(credential));

        credentialService.deletePermanently(7L);

        verify(credentialHistoryRepository).deleteByCredentialId(7L);
        verify(credentialRepository).delete(credential);
    }

    @Test
    void deletePermanentlyFailsWhenCredentialIsNotInTrash() {
        when(credentialRepository.findByIdAndDeletedTrue(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> credentialService.deletePermanently(8L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("8");
    }

    @Test
    void findAllDeletedUsesRepositoryOrder() {
        Credential c = new Credential();
        when(credentialRepository.findByDeletedTrueOrderByDeletedAtDesc()).thenReturn(List.of(c));

        List<Credential> deleted = credentialService.findAllDeleted();

        assertThat(deleted).containsExactly(c);
    }
}
