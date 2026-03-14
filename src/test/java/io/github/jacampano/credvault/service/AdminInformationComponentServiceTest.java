package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.catalog.InformationComponent;
import io.github.jacampano.credvault.domain.catalog.InformationSystem;
import io.github.jacampano.credvault.dto.admin.InformationComponentForm;
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
class AdminInformationComponentServiceTest {

    @Mock
    private InformationComponentRepository informationComponentRepository;

    @Mock
    private InformationSystemRepository informationSystemRepository;

    @InjectMocks
    private AdminInformationComponentService adminInformationComponentService;

    @Test
    void createSavesComponentWithSystem() {
        InformationSystem system = new InformationSystem();
        system.setId(3L);
        InformationComponentForm form = new InformationComponentForm();
        form.setName(" API ");
        form.setIdentifier(" API_001 ");
        form.setInformationSystemId(3L);
        when(informationComponentRepository.existsByNameIgnoreCase("API")).thenReturn(false);
        when(informationComponentRepository.existsByIdentifierIgnoreCase("API_001")).thenReturn(false);
        when(informationSystemRepository.findById(3L)).thenReturn(Optional.of(system));

        adminInformationComponentService.create(form);

        ArgumentCaptor<InformationComponent> captor = ArgumentCaptor.forClass(InformationComponent.class);
        verify(informationComponentRepository).save(captor.capture());
        assertThat(captor.getValue().getInformationSystem().getId()).isEqualTo(3L);
        assertThat(captor.getValue().getIdentifier()).isEqualTo("API_001");
    }

    @Test
    void createFailsWhenIdentifierExists() {
        InformationComponentForm form = new InformationComponentForm();
        form.setName("API");
        form.setIdentifier("API_001");
        form.setInformationSystemId(3L);
        when(informationComponentRepository.existsByNameIgnoreCase("API")).thenReturn(false);
        when(informationComponentRepository.existsByIdentifierIgnoreCase("API_001")).thenReturn(true);

        assertThatThrownBy(() -> adminInformationComponentService.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador");
    }

    @Test
    void updateFailsWhenSystemMissing() {
        InformationComponent component = new InformationComponent();
        component.setId(5L);
        InformationComponentForm form = new InformationComponentForm();
        form.setName("Comp");
        form.setIdentifier("COMP_1");
        form.setInformationSystemId(99L);
        when(informationComponentRepository.findById(5L)).thenReturn(Optional.of(component));
        when(informationComponentRepository.existsByNameIgnoreCaseAndIdNot("Comp", 5L)).thenReturn(false);
        when(informationComponentRepository.existsByIdentifierIgnoreCaseAndIdNot("COMP_1", 5L)).thenReturn(false);
        when(informationSystemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminInformationComponentService.update(5L, form))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }
}
