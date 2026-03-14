package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.catalog.InformationComponent;
import io.github.jacampano.credvault.dto.admin.InformationComponentForm;
import io.github.jacampano.credvault.service.AdminInformationComponentService;
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
@RequestMapping("/admin/components")
public class AdminInformationComponentController {

    private final AdminInformationComponentService adminInformationComponentService;
    private final AdminInformationSystemService adminInformationSystemService;

    public AdminInformationComponentController(AdminInformationComponentService adminInformationComponentService,
                                               AdminInformationSystemService adminInformationSystemService) {
        this.adminInformationComponentService = adminInformationComponentService;
        this.adminInformationSystemService = adminInformationSystemService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("components", adminInformationComponentService.findAll());
        return "admin/components/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new InformationComponentForm());
        model.addAttribute("systems", adminInformationSystemService.findAll());
        return "admin/components/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") InformationComponentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("systems", adminInformationSystemService.findAll());
            return "admin/components/create";
        }
        try {
            adminInformationComponentService.create(form);
            redirectAttributes.addFlashAttribute("message", "Componente creado correctamente");
            return "redirect:/admin/components";
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("systems", adminInformationSystemService.findAll());
            return "admin/components/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            InformationComponent component = adminInformationComponentService.findById(id);
            model.addAttribute("form", adminInformationComponentService.toForm(component));
            model.addAttribute("componentId", id);
            model.addAttribute("systems", adminInformationSystemService.findAll());
            return "admin/components/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/components";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") InformationComponentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("componentId", id);
            model.addAttribute("systems", adminInformationSystemService.findAll());
            return "admin/components/edit";
        }
        try {
            adminInformationComponentService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "Componente actualizado correctamente");
            return "redirect:/admin/components";
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("componentId", id);
            model.addAttribute("systems", adminInformationSystemService.findAll());
            return "admin/components/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            adminInformationComponentService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Componente eliminado");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/components";
    }
}
