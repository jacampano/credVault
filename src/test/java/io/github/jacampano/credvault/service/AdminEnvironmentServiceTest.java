package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.catalog.AppEnvironment;
import io.github.jacampano.credvault.dto.admin.AppEnvironmentForm;
import io.github.jacampano.credvault.repository.catalog.AppEnvironmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEnvironmentServiceTest {

    @Mock
    private AppEnvironmentRepository appEnvironmentRepository;

    @InjectMocks
    private AdminEnvironmentService adminEnvironmentService;

    @Test
    void createSavesEnvironmentWhenValid() {
        AppEnvironmentForm form = new AppEnvironmentForm();
        form.setName(" Producción ");
        form.setIdentifier(" PRO ");
        form.setDescription(" Entorno productivo ");
        when(appEnvironmentRepository.existsByNameIgnoreCase("Producción")).thenReturn(false);
        when(appEnvironmentRepository.existsByIdentifierIgnoreCase("PRO")).thenReturn(false);

        adminEnvironmentService.create(form);

        ArgumentCaptor<AppEnvironment> captor = ArgumentCaptor.forClass(AppEnvironment.class);
        verify(appEnvironmentRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Producción");
        assertThat(captor.getValue().getIdentifier()).isEqualTo("PRO");
    }

    @Test
    void createFailsWhenIdentifierExists() {
        AppEnvironmentForm form = new AppEnvironmentForm();
        form.setName("Producción");
        form.setIdentifier("PRO");
        when(appEnvironmentRepository.existsByNameIgnoreCase("Producción")).thenReturn(false);
        when(appEnvironmentRepository.existsByIdentifierIgnoreCase("PRO")).thenReturn(true);

        assertThatThrownBy(() -> adminEnvironmentService.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(appEnvironmentRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEnvironmentService.findById(77L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("77");
    }
}
