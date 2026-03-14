package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.catalog.AppEnvironment;
import io.github.jacampano.credvault.dto.admin.AppEnvironmentForm;
import io.github.jacampano.credvault.service.AdminEnvironmentService;
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
class AdminEnvironmentControllerTest {

    @Mock
    private AdminEnvironmentService adminEnvironmentService;

    @InjectMocks
    private AdminEnvironmentController controller;

    @Test
    void listLoadsView() {
        when(adminEnvironmentService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.list(model);

        assertThat(view).isEqualTo("admin/environments/list");
        assertThat(model.getAttribute("environments")).isNotNull();
    }

    @Test
    void createRedirectsWhenSuccess() {
        AppEnvironmentForm form = new AppEnvironmentForm();
        form.setName("Producción");
        form.setIdentifier("PRO");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.create(form, binding, redirect);

        assertThat(view).isEqualTo("redirect:/admin/environments");
        verify(adminEnvironmentService).create(form);
    }

    @Test
    void editRedirectsWhenMissing() {
        when(adminEnvironmentService.findById(4L)).thenThrow(new EntityNotFoundException("not found"));
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.edit(4L, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/environments");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void deleteRedirectsWithErrorWhenMissing() {
        doThrow(new EntityNotFoundException("not found")).when(adminEnvironmentService).delete(6L);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.delete(6L, redirect);

        assertThat(view).isEqualTo("redirect:/admin/environments");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void editLoadsFormWhenFound() {
        AppEnvironment environment = new AppEnvironment();
        environment.setId(3L);
        AppEnvironmentForm form = new AppEnvironmentForm();
        when(adminEnvironmentService.findById(3L)).thenReturn(environment);
        when(adminEnvironmentService.toForm(environment)).thenReturn(form);
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.edit(3L, model, redirect);

        assertThat(view).isEqualTo("admin/environments/edit");
        assertThat(model.getAttribute("form")).isEqualTo(form);
    }
}
