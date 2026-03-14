package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.catalog.InformationSystem;
import io.github.jacampano.credvault.dto.admin.InformationSystemForm;
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
class AdminInformationSystemControllerTest {

    @Mock
    private AdminInformationSystemService adminInformationSystemService;

    @InjectMocks
    private AdminInformationSystemController controller;

    @Test
    void listLoadsView() {
        when(adminInformationSystemService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.list(model);

        assertThat(view).isEqualTo("admin/information-systems/list");
        assertThat(model.getAttribute("systems")).isNotNull();
    }

    @Test
    void createRedirectsWhenSuccess() {
        InformationSystemForm form = new InformationSystemForm();
        form.setName("S");
        form.setIdentifier("ID");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.create(form, binding, redirect);

        assertThat(view).isEqualTo("redirect:/admin/information-systems");
        verify(adminInformationSystemService).create(form);
    }

    @Test
    void editRedirectsWhenMissing() {
        when(adminInformationSystemService.findById(4L)).thenThrow(new EntityNotFoundException("not found"));
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.edit(4L, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/information-systems");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void deleteRedirectsWithErrorOnBusinessFailure() {
        doThrow(new IllegalArgumentException("has components")).when(adminInformationSystemService).delete(5L);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.delete(5L, redirect);

        assertThat(view).isEqualTo("redirect:/admin/information-systems");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void editLoadsFormWhenFound() {
        InformationSystem system = new InformationSystem();
        system.setId(7L);
        InformationSystemForm form = new InformationSystemForm();
        when(adminInformationSystemService.findById(7L)).thenReturn(system);
        when(adminInformationSystemService.toForm(system)).thenReturn(form);
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.edit(7L, model, redirect);

        assertThat(view).isEqualTo("admin/information-systems/edit");
        assertThat(model.getAttribute("form")).isEqualTo(form);
    }
}
