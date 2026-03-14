package io.github.jacampano.credvault;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.domain.CredentialType;
import io.github.jacampano.credvault.domain.catalog.AppEnvironment;
import io.github.jacampano.credvault.domain.catalog.InformationComponent;
import io.github.jacampano.credvault.domain.catalog.InformationSystem;
import io.github.jacampano.credvault.repository.CredentialRepository;
import io.github.jacampano.credvault.repository.catalog.AppEnvironmentRepository;
import io.github.jacampano.credvault.repository.catalog.InformationComponentRepository;
import io.github.jacampano.credvault.repository.catalog.InformationSystemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "app.crypto.key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.datasource.url=jdbc:h2:mem:credvault_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CredentialEncryptionIntegrationTest {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InformationSystemRepository informationSystemRepository;

    @Autowired
    private InformationComponentRepository informationComponentRepository;

    @Autowired
    private AppEnvironmentRepository appEnvironmentRepository;

    @Test
    void shouldStoreEncryptedValuesInDatabase() {
        Credential credential = new Credential();
        InformationSystem informationSystem = new InformationSystem();
        informationSystem.setName("Sistema Test");
        informationSystem.setIdentifier("SIST_TEST");
        informationSystem = informationSystemRepository.save(informationSystem);

        InformationComponent component = new InformationComponent();
        component.setName("Componente Test");
        component.setIdentifier("COMP_TEST");
        component.setInformationSystem(informationSystem);
        component = informationComponentRepository.save(component);

        AppEnvironment environment = new AppEnvironment();
        environment.setName("Producción");
        environment.setIdentifier("PRO");
        environment = appEnvironmentRepository.save(environment);

        credential.setIdentifier("GitHub");
        credential.setCreatedBy("admin");
        credential.setInformationComponent(component);
        credential.setEnvironment(environment);
        credential.setType(CredentialType.WEB_USER_PASSWORD);
        credential.setTeams(java.util.Set.of("devops"));
        credential.setShared(true);
        credential.setWebUsername("plain-user");
        credential.setWebPassword("plain-pass");
        credential.setWebUrl("https://example.org");
        credential.setNotes("texto secreto");

        Credential saved = credentialRepository.save(credential);

        String dbUsername = jdbcTemplate.queryForObject(
                "select secret_web_username from credentials where id = ?",
                String.class,
                saved.getId());
        String dbPassword = jdbcTemplate.queryForObject(
                "select secret_web_password from credentials where id = ?",
                String.class,
                saved.getId());

        assertThat(dbUsername).isNotEqualTo("plain-user");
        assertThat(dbPassword).isNotEqualTo("plain-pass");

        Credential loaded = credentialRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getWebUsername()).isEqualTo("plain-user");
        assertThat(loaded.getWebPassword()).isEqualTo("plain-pass");
    }
}
