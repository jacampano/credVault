package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.auth.Team;
import io.github.jacampano.credvault.dto.admin.TeamForm;
import io.github.jacampano.credvault.service.AdminTeamService;
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
@RequestMapping("/admin/teams")
public class AdminTeamController {

    private final AdminTeamService adminTeamService;

    public AdminTeamController(AdminTeamService adminTeamService) {
        this.adminTeamService = adminTeamService;
    }

    @GetMapping
    public String listTeams(Model model) {
        model.addAttribute("teams", adminTeamService.findAll());
        return "admin/teams/list";
    }

    @GetMapping("/new")
    public String newTeamForm(Model model) {
        model.addAttribute("form", new TeamForm());
        return "admin/teams/create";
    }

    @PostMapping
    public String createTeam(@Valid @ModelAttribute("form") TeamForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/teams/create";
        }
        try {
            adminTeamService.createTeam(form);
            redirectAttributes.addFlashAttribute("message", "Equipo creado correctamente");
            return "redirect:/admin/teams";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            return "admin/teams/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editTeam(@PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Team team = adminTeamService.findById(id);
            model.addAttribute("form", adminTeamService.toForm(team));
            model.addAttribute("teamId", id);
            return "admin/teams/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/teams";
        }
    }

    @PostMapping("/{id}")
    public String updateTeam(@PathVariable Long id,
                             @Valid @ModelAttribute("form") TeamForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("teamId", id);
            return "admin/teams/edit";
        }
        try {
            adminTeamService.updateTeam(id, form);
            redirectAttributes.addFlashAttribute("message", "Equipo actualizado correctamente");
            return "redirect:/admin/teams";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("teamId", id);
            return "admin/teams/edit";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/teams";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteTeam(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            adminTeamService.deleteTeam(id);
            redirectAttributes.addFlashAttribute("message", "Equipo eliminado");
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/teams";
    }
}
