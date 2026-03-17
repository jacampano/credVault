package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.domain.CredentialType;
import io.github.jacampano.credvault.service.CredentialService;
import io.github.jacampano.credvault.service.UserAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/calendar")
public class CalendarWebController {

    private static final Locale ES = new Locale("es", "ES");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", ES);
    private static final List<String> WEEKDAYS = List.of("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom");

    private final CredentialService credentialService;
    private final UserAccessService userAccessService;

    public CalendarWebController(CredentialService credentialService,
                                 UserAccessService userAccessService) {
        this.credentialService = credentialService;
        this.userAccessService = userAccessService;
    }

    @GetMapping
    public String view(@RequestParam(name = "year", required = false) Integer year,
                       @RequestParam(name = "month", required = false) Integer month,
                       Authentication authentication,
                       Model model) {
        LocalDate today = LocalDate.now();
        YearMonth selectedMonth = resolveMonth(year, month, today);
        Set<String> userGroups = credentialService.normalizeGroups(userAccessService.getGroupsForUser(authentication.getName()));

        List<Credential> visibleCredentials = credentialService.findAllVisibleForUser(authentication.getName(), userGroups);
        List<CredentialExpiration> expirations = visibleCredentials.stream()
                .filter(c -> c.getType() == CredentialType.TOKEN)
                .filter(c -> !c.isTokenNoExpiry())
                .filter(c -> c.getTokenExpirationDate() != null)
                .map(c -> new CredentialExpiration(c.getIdentifier(), c.getTokenExpirationDate()))
                .sorted(Comparator.comparing(CredentialExpiration::expirationDate)
                        .thenComparing(CredentialExpiration::identifier, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<LocalDate, List<CredentialExpiration>> byDate = expirations.stream()
                .collect(Collectors.groupingBy(CredentialExpiration::expirationDate,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<CalendarDay> days = buildDays(selectedMonth, byDate, today);
        List<CredentialExpiration> upcoming = expirations.stream()
                .filter(item -> !item.expirationDate().isBefore(today))
                .limit(15)
                .toList();

        YearMonth previous = selectedMonth.minusMonths(1);
        YearMonth next = selectedMonth.plusMonths(1);
        String monthLabel = selectedMonth.format(MONTH_FORMATTER);
        monthLabel = monthLabel.substring(0, 1).toUpperCase(ES) + monthLabel.substring(1);

        model.addAttribute("weekdays", WEEKDAYS);
        model.addAttribute("calendarDays", days);
        model.addAttribute("upcomingExpirations", upcoming);
        model.addAttribute("monthLabel", monthLabel);
        model.addAttribute("currentMonth", selectedMonth.getMonth().getDisplayName(TextStyle.FULL, ES));
        model.addAttribute("currentYear", selectedMonth.getYear());
        model.addAttribute("previousYear", previous.getYear());
        model.addAttribute("previousMonth", previous.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());
        return "calendar/view";
    }

    private YearMonth resolveMonth(Integer year, Integer month, LocalDate today) {
        if (year == null || month == null) {
            return YearMonth.from(today);
        }
        try {
            return YearMonth.of(year, month);
        } catch (RuntimeException ex) {
            return YearMonth.from(today);
        }
    }

    private List<CalendarDay> buildDays(YearMonth selectedMonth,
                                        Map<LocalDate, List<CredentialExpiration>> byDate,
                                        LocalDate today) {
        LocalDate firstGridDay = selectedMonth.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastGridDay = selectedMonth.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<CalendarDay> result = new ArrayList<>();
        for (LocalDate date = firstGridDay; !date.isAfter(lastGridDay); date = date.plusDays(1)) {
            List<CredentialExpiration> dayExpirations = byDate.getOrDefault(date, List.of());
            Set<String> names = dayExpirations.stream()
                    .map(CredentialExpiration::identifier)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            String tooltip = names.isEmpty() ? "" : String.join(", ", names);
            result.add(new CalendarDay(
                    date,
                    date.getMonthValue() == selectedMonth.getMonthValue(),
                    !dayExpirations.isEmpty(),
                    date.isBefore(today),
                    tooltip
            ));
        }
        return result;
    }

    public record CalendarDay(LocalDate date,
                              boolean inCurrentMonth,
                              boolean hasExpirations,
                              boolean inPast,
                              String tooltip) {
        public String dayNumber() {
            return String.valueOf(date.getDayOfMonth());
        }

        public String fullDate() {
            return date.format(DAY_FORMATTER);
        }
    }

    public record CredentialExpiration(String identifier, LocalDate expirationDate) {
        public String formattedExpirationDate() {
            return expirationDate.format(DAY_FORMATTER);
        }
    }
}
