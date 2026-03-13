package com.example.credvault;

import com.example.credvault.domain.Credential;
import com.example.credvault.repository.CredentialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "app.crypto.key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class CredentialEncryptionIntegrationTest {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldStoreEncryptedValuesInDatabase() {
        Credential credential = new Credential();
        credential.setName("GitHub");
        credential.setCreatedBy("admin");
        credential.setTeam("devops");
        credential.setShared(true);
        credential.setUsername("plain-user");
        credential.setPassword("plain-pass");
        credential.setUrl("https://example.org");
        credential.setNotes("texto secreto");

        Credential saved = credentialRepository.save(credential);

        String dbUsername = jdbcTemplate.queryForObject(
                "select secret_username from credentials where id = ?",
                String.class,
                saved.getId());
        String dbPassword = jdbcTemplate.queryForObject(
                "select secret_password from credentials where id = ?",
                String.class,
                saved.getId());

        assertThat(dbUsername).isNotEqualTo("plain-user");
        assertThat(dbPassword).isNotEqualTo("plain-pass");

        Credential loaded = credentialRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getUsername()).isEqualTo("plain-user");
        assertThat(loaded.getPassword()).isEqualTo("plain-pass");
    }
}
