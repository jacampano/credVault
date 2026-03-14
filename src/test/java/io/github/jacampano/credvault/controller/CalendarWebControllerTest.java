package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.domain.CredentialType;
import io.github.jacampano.credvault.service.CredentialService;
import io.github.jacampano.credvault.service.UserAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarWebControllerTest {

    @Mock
    private CredentialService credentialService;

    @Mock
    private UserAccessService userAccessService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CalendarWebController calendarWebController;

    @Test
    void viewBuildsCalendarAndUpcomingExpirations() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));

        LocalDate today = LocalDate.now();
        Credential tokenSoon = tokenCredential("api-token", today.plusDays(2), false);
        Credential tokenPast = tokenCredential("old-token", today.minusDays(5), false);
        Credential nonExpiringToken = tokenCredential("never", null, true);
        Credential webCredential = new Credential();
        webCredential.setType(CredentialType.WEB_USER_PASSWORD);
        webCredential.setIdentifier("web");

        when(credentialService.findAllVisibleForUser("ana", Set.of("DEVOPS")))
                .thenReturn(List.of(tokenSoon, tokenPast, nonExpiringToken, webCredential));
        Model model = new ExtendedModelMap();

        String view = calendarWebController.view(null, null, authentication, model);

        assertThat(view).isEqualTo("calendar/view");
        assertThat(model.getAttribute("calendarDays")).isNotNull();
        @SuppressWarnings("unchecked")
        List<CalendarWebController.CredentialExpiration> upcoming =
                (List<CalendarWebController.CredentialExpiration>) model.getAttribute("upcomingExpirations");
        assertThat(upcoming).extracting(CalendarWebController.CredentialExpiration::identifier)
                .containsExactly("api-token");
        verify(credentialService).findAllVisibleForUser("ana", Set.of("DEVOPS"));
    }

    @Test
    void viewFallsBackToCurrentMonthWhenParamsInvalid() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of());
        when(credentialService.normalizeTeams(Set.of())).thenReturn(Set.of());
        when(credentialService.findAllVisibleForUser("ana", Set.of())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = calendarWebController.view(2026, 77, authentication, model);

        assertThat(view).isEqualTo("calendar/view");
        assertThat(model.getAttribute("currentYear")).isEqualTo(LocalDate.now().getYear());
    }

    private Credential tokenCredential(String identifier, LocalDate expiration, boolean noExpiry) {
        Credential credential = new Credential();
        credential.setType(CredentialType.TOKEN);
        credential.setIdentifier(identifier);
        credential.setTokenExpirationDate(expiration);
        credential.setTokenNoExpiry(noExpiry);
        return credential;
    }
}
