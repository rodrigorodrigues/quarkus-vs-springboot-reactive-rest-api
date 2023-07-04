package com.github.springboot;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springboot.config.SpringSecurityAuditorAware;
import com.github.springboot.dto.AuthorizationDto;
import com.github.springboot.dto.CompanyDto;
import com.github.springboot.model.Company;
import com.github.springboot.repository.CompanyRepository;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SpringBootServiceApplication.class,
		properties = {"configuration.swagger=false",
            "logging.level.root=debug"})
@AutoConfigureWebTestClient(timeout = "1s")
@AutoConfigureWireMock(port = 0)
@ContextConfiguration(initializers = SpringBootServiceApplication.InitPublicKeyConfiguration.class)
public class SpringBootServiceApplicationIntegrationTest {
	@Autowired
    WebTestClient client;

	@Autowired
    ObjectMapper objectMapper;

	@Autowired
    CompanyRepository companyRepository;

	@Autowired
    SpringSecurityAuditorAware springSecurityAuditorAware;

	@Autowired
    OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

	@Autowired
    KeyPair keyPair;

	Company company;

	Map<String, List<GrantedAuthority>> users = new HashMap<>();

    {
        users.put("default@admin.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        users.put("anonymous@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_COMPANY_READ")));
        users.put("master@gmail.com", Arrays.asList(new SimpleGrantedAuthority("ROLE_COMPANY_CREATE"),
            new SimpleGrantedAuthority("ROLE_COMPANY_READ"),
            new SimpleGrantedAuthority("ROLE_COMPANY_SAVE")));
    }

    @BeforeEach
    public void setup() {
        RSAKey.Builder builder = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .keyID("test");
        JWKSet jwkSet = new JWKSet(builder.build());

        String jsonPublicKey = jwkSet.toString();
        log.debug("jsonPublicKey: {}", jsonPublicKey);
        stubFor(get(anyUrl())
            .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(jsonPublicKey)));

        springSecurityAuditorAware.setCurrentAuthenticatedUser(new TestingAuthenticationToken("master@gmail.com", "pass",
            AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

        companyRepository.save(Company.builder()
            .name("Company Master")
            .activated(true)
            .createdByUser("master@gmail.com")
            .build())
            .subscribe(p -> this.company = p);
    }

    @AfterEach
    public void tearDown() {
        companyRepository.delete(company).subscribe(p -> log.debug("Deleted company entity"));
    }

    @Test
	@DisplayName("Test - When Calling GET - /api/companies should return filter list of companies and response 200 - OK")
	public void shouldReturnListOfCompaniesWhenCallApi() {
		String authorizationHeader = authorizationHeader("master@gmail.com");

        client.get().uri("/api/companies")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.exchange()
				.expectStatus().isOk()
                .expectBodyList(CompanyDto.class).hasSize(1);

        client.mutateWith(mockJwt().authorities(AuthorityUtils.createAuthorityList("ROLE_COMPANY_READ"))).get().uri("/api/companies")
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody()
            .subscribe(json -> {
                log.debug("output: {}", json);
                assertThat((String) JsonPath.read(json, "$.createdByUser")).isEqualTo("master@gmail.com");
                assertThat((Object) JsonPath.read(json, "$.createdDate")).isNotNull();
            });
	}

    @Test
    @DisplayName("Test - When Calling GET - /api/companies should return list of companies and response 200 - OK")
    public void shouldReturnListOfAllCompaniesWhenCallApi() {
        String authorizationHeader = authorizationHeader("default@admin.com");

        client.get().uri("/api/companies")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(CompanyDto.class).hasSize(4);
    }

	@Test
    @DisplayName("Test - When Calling POST - /api/companies should create a new company and response 201 - Created")
	public void shouldInsertNewCompanyWhenCallApi() throws Exception {
		String authorizationHeader = authorizationHeader("master@gmail.com");
		CompanyDto company = createCompany();

		client.post().uri("/api/companies")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromValue(convertToJson(company)))
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectHeader().value(HttpHeaders.LOCATION, containsString("/api/companies/"))
				.expectBody()
                    .jsonPath("$.id").isNotEmpty()
                    .jsonPath("$.createdByUser").isEqualTo("master@gmail.com")
                    .consumeWith(c -> setId(company, c));

		assertThat(company.getId()).isNotEmpty();

		client.delete().uri("/api/companies/{id}", company.getId())
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader("default@admin.com"))
            .exchange()
            .expectStatus().is2xxSuccessful();
	}

    @Test
    @DisplayName("Test - When Calling POST - /api/companies without mandatory field should response 400 - Bad Request")
	public void shouldResponseBadRequestWhenCallApiWithoutValidRequest() throws JsonProcessingException {
		String authorizationHeader = authorizationHeader("default@admin.com");

		CompanyDto companyDto = createCompany();
		companyDto.setName("");

		client.post().uri("/api/companies")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromValue(convertToJson(companyDto)))
				.exchange()
				.expectStatus().is4xxClientError()
				.expectBody().jsonPath("$.message").value(containsString("must not be blank"));
	}

	@Test
    @DisplayName("Test - When Calling POST - /api/companies without valid authorization should response 403 - Forbidden")
	public void shouldResponseForbiddenWhenCallApiWithoutRightPermission() throws Exception {
		String authorizationHeader = authorizationHeader("anonymous@gmail.com");

		CompanyDto company = createCompany();

		client.post().uri("/api/companies")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.body(fromValue(convertToJson(company)))
				.exchange()
				.expectStatus().isForbidden();
	}

	@SneakyThrows
    private String authorizationHeader(String user) {
        if (users.containsKey(user)) {
            String[] roles = users.get(user)
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
                .toArray(new String[] {});

            EntityExchangeResult<byte[]> response = client.post().uri("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(new AuthorizationDto(user, roles)))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .returnResult();

            return (String) objectMapper.readValue(response.getResponseBody(), Map.class)
                .get("token");
        } else {
            return null;
        }
	}

	private CompanyDto createCompany() {
		return CompanyDto.builder().name("Company Test")
			.activated(true)
			.build();
	}

	private String convertToJson(CompanyDto companyDto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(companyDto);
	}

    private void setId(CompanyDto companyDto, EntityExchangeResult<byte[]> c) {
        try {
            companyDto.setId(objectMapper.readValue(c.getResponseBody(), CompanyDto.class).getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
