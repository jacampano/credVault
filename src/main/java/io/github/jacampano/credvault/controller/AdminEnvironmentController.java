package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.catalog.AppEnvironment;
import io.github.jacampano.credvault.dto.admin.AppEnvironmentForm;
import io.github.jacampano.credvault.service.AdminEnvironmentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/environments")
public class AdminEnvironmentController {

    private final AdminEnvironmentService adminEnvironmentService;

    public AdminEnvironmentController(AdminEnvironmentService adminEnvironmentService) {
        this.adminEnvironmentService = adminEnvironmentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("environments", adminEnvironmentService.findAll());
        return "admin/environments/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new AppEnvironmentForm());
        return "admin/environments/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AppEnvironmentForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/environments/create";
        }
        try {
            adminEnvironmentService.create(form);
            redirectAttributes.addFlashAttribute("message", "Entorno creado correctamente");
            return "redirect:/admin/environments";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            return "admin/environments/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            AppEnvironment environment = adminEnvironmentService.findById(id);
            model.addAttribute("form", adminEnvironmentService.toForm(environment));
            model.addAttribute("environmentId", id);
            return "admin/environments/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/environments";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") AppEnvironmentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("environmentId", id);
            return "admin/environments/edit";
        }
        try {
            adminEnvironmentService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "Entorno actualizado correctamente");
            return "redirect:/admin/environments";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("environmentId", id);
            return "admin/environments/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/environments";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            adminEnvironmentService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Entorno eliminado");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/environments";
    }
}
