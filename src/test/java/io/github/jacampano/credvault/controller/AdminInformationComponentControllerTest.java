package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.catalog.InformationComponent;
import io.github.jacampano.credvault.dto.admin.InformationComponentForm;
import io.github.jacampano.credvault.service.AdminInformationComponentService;
import io.github.jacampano.credvault.service.AdminInformationSystemService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInformationComponentControllerTest {

    @Mock
    private AdminInformationComponentService adminInformationComponentService;

    @Mock
    private AdminInformationSystemService adminInformationSystemService;

    @InjectMocks
    private AdminInformationComponentController controller;

    @Test
    void listLoadsView() {
        when(adminInformationComponentService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.list(model);

        assertThat(view).isEqualTo("admin/components/list");
        assertThat(model.getAttribute("components")).isNotNull();
    }

    @Test
    void newFormLoadsSystems() {
        when(adminInformationSystemService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.newForm(model);

        assertThat(view).isEqualTo("admin/components/create");
        assertThat(model.getAttribute("systems")).isNotNull();
    }

    @Test
    void createRedirectsWhenSuccess() {
        InformationComponentForm form = new InformationComponentForm();
        form.setName("C");
        form.setIdentifier("CID");
        form.setInformationSystemId(1L);
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.create(form, binding, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/components");
        verify(adminInformationComponentService).create(form);
    }

    @Test
    void editRedirectsWhenMissing() {
        when(adminInformationComponentService.findById(9L)).thenThrow(new EntityNotFoundException("not found"));
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.edit(9L, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/components");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void deleteRedirectsWithErrorWhenMissing() {
        doThrow(new EntityNotFoundException("not found")).when(adminInformationComponentService).delete(5L);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.delete(5L, redirect);

        assertThat(view).isEqualTo("redirect:/admin/components");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void editLoadsFormWhenFound() {
        InformationComponent component = new InformationComponent();
        component.setId(2L);
        InformationComponentForm form = new InformationComponentForm();
        when(adminInformationComponentService.findById(2L)).thenReturn(component);
        when(adminInformationComponentService.toForm(component)).thenReturn(form);
        when(adminInformationSystemService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.edit(2L, model, redirect);

        assertThat(view).isEqualTo("admin/components/edit");
        assertThat(model.getAttribute("form")).isEqualTo(form);
    }
}
