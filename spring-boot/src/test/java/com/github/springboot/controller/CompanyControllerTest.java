package com.github.springboot.controller;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springboot.config.SpringSecurityAuditorAware;
import com.github.springboot.config.SpringSecurityConfiguration;
import com.github.springboot.dto.CompanyDto;
import com.github.springboot.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@ExtendWith(SpringExtension.class)
@WebFluxTest(properties = {
        "configuration.initialLoad=false",
        "configuration.mongo=false"},
controllers = CompanyController.class, excludeAutoConfiguration = MongoReactiveAutoConfiguration.class)
@Import({SpringSecurityConfiguration.class, ErrorWebFluxAutoConfiguration.class})
@AutoConfigureWireMock(port = 0)
public class CompanyControllerTest {

    @Autowired
    WebTestClient client;

    @MockBean
    CompanyService companyService;

    @MockBean
    SpringSecurityAuditorAware springSecurityAuditorAware;

    @MockBean
    RSAPublicKey publicKey;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        stubFor(get(anyUrl())
            .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody("Hello World!")));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/companies without valid authorization the response should be 403 - Forbidden")
    @WithMockUser(roles = "INVALID_ROLE")
    public void whenCallFindAllShouldReturnForbiddenWhenDoesNotHavePermission() {
        client.get().uri("/api/companies")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/companies without authorization the response should be 401 - Unauthorized")
    public void whenCallFindAllShouldReturnUnauthorizedWhenDoesNotHavePermission() {
        client.get().uri("/api/companies")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/companies with admin role the response should be a list of Companies - 200 - OK")
    @WithMockUser(roles = "ADMIN")
    public void whenCallFindAllShouldReturnListOfCompanies() {
        CompanyDto companyDto = new CompanyDto();
        companyDto.setId("100");
        CompanyDto companyDto1 = new CompanyDto();
        companyDto1.setId("200");
        when(companyService.findAllActiveCompanies(any())).thenReturn(Flux.fromIterable(Arrays.asList(companyDto, companyDto1)));

        ParameterizedTypeReference<ServerSentEvent<CompanyDto>> type = new ParameterizedTypeReference<ServerSentEvent<CompanyDto>>() {};

        client.get().uri("/api/companies")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE)
                .expectBodyList(type)
                .hasSize(2);
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/companies with COMPANY_read role the response should be filtered - 200 - OK")
    @WithMockUser(roles = "COMPANY_READ", username = "me")
    public void whenCallShouldFilterListOfCompanies() {
        CompanyDto companyDto = new CompanyDto();
        companyDto.setId("100");
        companyDto.setCreatedByUser("me");
        when(companyService.findActiveCompaniesByUser(anyString(), any())).thenReturn(Flux.fromIterable(Arrays.asList(companyDto)));

        ParameterizedTypeReference<ServerSentEvent<CompanyDto>> type = new ParameterizedTypeReference<ServerSentEvent<CompanyDto>>() {};

        client.get().uri("/api/companies")
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE)
            .expectBodyList(type)
            .hasSize(1);
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/companies/{id} with valid authorization the response should be company - 200 - OK")
    @WithMockUser(roles = "COMPANY_READ", username = "me")
    public void whenCallFindByIdShouldReturnCompany() {
        CompanyDto companyDto = new CompanyDto();
        companyDto.setId("100");
        companyDto.setCreatedByUser("me");
        when(companyService.findById(anyString())).thenReturn(Mono.just(companyDto));

        client.get().uri("/api/companies/{id}", 100)
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo("100"));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/companies/{id} with different user should response 403 - Forbidden")
    @WithMockUser(roles = "COMPANY_READ", username = "test")
    public void whenCallFindByIdShouldResponseForbidden() {
        CompanyDto companyDto = new CompanyDto();
        companyDto.setId("100");
        companyDto.setCreatedByUser("test1");
        when(companyService.findById(anyString())).thenReturn(Mono.just(companyDto));

        client.get().uri("/api/companies/{id}", 100)
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
            .exchange()
            .expectStatus().isForbidden()
            .expectBody().jsonPath("$.message").value(containsString("User(test) does not have access to this resource"));
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/companies with valid authorization the response should be a company - 201 - Created")
    @WithMockUser(roles = "COMPANY_CREATE")
    public void whenCallCreateShouldSaveCompany() throws Exception {
        CompanyDto companyDto = createCompanyDto();
        when(companyService.save(any(CompanyDto.class))).thenReturn(Mono.just(companyDto));

        client.post().uri("/api/companies")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(convertToJson(companyDto)))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo(companyDto.getId()));
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/companies/{id} with valid authorization the response should be a company - 200 - OK")
    @WithMockUser(roles = "COMPANY_SAVE")
    public void whenCallUpdateShouldUpdateCompany() throws Exception {
        CompanyDto companyDto = createCompanyDto();
        companyDto.setId(UUID.randomUUID().toString());
        companyDto.setName("New Name");
        when(companyService.findById(anyString())).thenReturn(Mono.just(companyDto));
        when(companyService.save(any(CompanyDto.class))).thenReturn(Mono.just(companyDto));

        client.put().uri("/api/companies/{id}", companyDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(convertToJson(companyDto)))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.id").value(equalTo(companyDto.getId()))
                .jsonPath("$.name").value(equalTo(companyDto.getName()));
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/companies/{id} with invalid id the response should be 404 - Not Found")
    @WithMockUser(roles = "COMPANY_SAVE")
    public void whenCallUpdateShouldResponseNotFound() throws Exception {
        CompanyDto companyDto = createCompanyDto();
        companyDto.setId("999");
        when(companyService.findById(anyString())).thenReturn(Mono.empty());

        client.put().uri("/api/companies/{id}", companyDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(convertToJson(companyDto)))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/companies/{id} with valid authorization the response should be 200 - OK")
    @WithMockUser(roles = "COMPANY_DELETE", username = "mock")
    public void whenCallDeleteShouldDeleteById() {
        CompanyDto companyDto = new CompanyDto();
        companyDto.setId("12345");
        companyDto.setCreatedByUser("mock");
        when(companyService.findById(anyString())).thenReturn(Mono.just(companyDto));
        when(companyService.deleteById(anyString())).thenReturn(Mono.empty());

        client.delete().uri("/api/companies/{id}", companyDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/companies/{id} with different user  the response should be 403 - Forbidden")
    @WithMockUser(roles = "COMPANY_DELETE", username = "test")
    public void whenCallDeleteWithDifferentUSerShouldResponseForbidden() {
        CompanyDto companyDto = new CompanyDto();
        companyDto.setId("12345");
        companyDto.setCreatedByUser("mock");
        when(companyService.findById(anyString())).thenReturn(Mono.just(companyDto));
        when(companyService.deleteById(anyString())).thenReturn(Mono.empty());

        client.delete().uri("/api/companies/{id}", companyDto.getId())
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
            .exchange()
            .expectStatus().isForbidden()
            .expectBody().jsonPath("$.message").value(containsString("User(test) does not have access to delete this resource"));
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/companies/{id} with id that does not exist should response 404 - Not Found")
    @WithMockUser(roles = "COMPANY_DELETE")
    public void whenCallDeleteShouldResponseNotFound() {
        when(companyService.findById(anyString())).thenReturn(Mono.empty());

        client.delete().uri("/api/companies/{id}", "12345")
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
            .exchange()
            .expectStatus().isNotFound();

        verify(companyService, never()).deleteById(anyString());
    }

    private String convertToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private CompanyDto createCompanyDto() {
        return CompanyDto.builder()
                .id(UUID.randomUUID().toString())
                .activated(true)
                .name("Test")
                .build();
    }
}
