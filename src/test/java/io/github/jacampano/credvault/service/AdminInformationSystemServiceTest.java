package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.catalog.InformationSystem;
import io.github.jacampano.credvault.dto.admin.InformationSystemForm;
import io.github.jacampano.credvault.repository.catalog.InformationComponentRepository;
import io.github.jacampano.credvault.repository.catalog.InformationSystemRepository;
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
class AdminInformationSystemServiceTest {

    @Mock
    private InformationSystemRepository informationSystemRepository;

    @Mock
    private InformationComponentRepository informationComponentRepository;

    @InjectMocks
    private AdminInformationSystemService adminInformationSystemService;

    @Test
    void createSavesSystemWhenValid() {
        InformationSystemForm form = new InformationSystemForm();
        form.setName(" Sistema X ");
        form.setIdentifier(" SYS_X ");
        form.setDescription(" Descripción ");
        when(informationSystemRepository.existsByNameIgnoreCase("Sistema X")).thenReturn(false);
        when(informationSystemRepository.existsByIdentifierIgnoreCase("SYS_X")).thenReturn(false);

        adminInformationSystemService.create(form);

        ArgumentCaptor<InformationSystem> captor = ArgumentCaptor.forClass(InformationSystem.class);
        verify(informationSystemRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Sistema X");
        assertThat(captor.getValue().getIdentifier()).isEqualTo("SYS_X");
    }

    @Test
    void createFailsWhenIdentifierExists() {
        InformationSystemForm form = new InformationSystemForm();
        form.setName("Sistema X");
        form.setIdentifier("SYS_X");
        when(informationSystemRepository.existsByNameIgnoreCase("Sistema X")).thenReturn(false);
        when(informationSystemRepository.existsByIdentifierIgnoreCase("SYS_X")).thenReturn(true);

        assertThatThrownBy(() -> adminInformationSystemService.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador");
    }

    @Test
    void deleteFailsWhenHasComponents() {
        InformationSystem system = new InformationSystem();
        system.setId(2L);
        when(informationSystemRepository.findById(2L)).thenReturn(Optional.of(system));
        when(informationComponentRepository.countByInformationSystemId(2L)).thenReturn(1L);

        assertThatThrownBy(() -> adminInformationSystemService.delete(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("componentes");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(informationSystemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminInformationSystemService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }
}
