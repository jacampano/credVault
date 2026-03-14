package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.catalog.InformationSystem;
import io.github.jacampano.credvault.dto.admin.InformationSystemForm;
import io.github.jacampano.credvault.service.AdminInformationSystemService;
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
@RequestMapping("/admin/information-systems")
public class AdminInformationSystemController {

    private final AdminInformationSystemService adminInformationSystemService;

    public AdminInformationSystemController(AdminInformationSystemService adminInformationSystemService) {
        this.adminInformationSystemService = adminInformationSystemService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("systems", adminInformationSystemService.findAll());
        return "admin/information-systems/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new InformationSystemForm());
        return "admin/information-systems/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") InformationSystemForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/information-systems/create";
        }
        try {
            adminInformationSystemService.create(form);
            redirectAttributes.addFlashAttribute("message", "Sistema de información creado correctamente");
            return "redirect:/admin/information-systems";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            return "admin/information-systems/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            InformationSystem system = adminInformationSystemService.findById(id);
            model.addAttribute("form", adminInformationSystemService.toForm(system));
            model.addAttribute("systemId", id);
            return "admin/information-systems/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/information-systems";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") InformationSystemForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("systemId", id);
            return "admin/information-systems/edit";
        }
        try {
            adminInformationSystemService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "Sistema de información actualizado correctamente");
            return "redirect:/admin/information-systems";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("systemId", id);
            return "admin/information-systems/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/information-systems";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            adminInformationSystemService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Sistema de información eliminado");
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/information-systems";
    }
}
