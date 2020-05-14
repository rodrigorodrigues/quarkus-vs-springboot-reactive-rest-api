package com.github.springboot.controller;

import java.net.URI;

import javax.validation.Valid;

import com.github.springboot.config.SpringSecurityAuditorAware;
import com.github.springboot.dto.CompanyDto;
import com.github.springboot.service.CompanyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import springfox.documentation.annotations.ApiIgnore;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rest API for companies.
 */
@Slf4j
@RestController
@Api(value = "companies", description = "Methods for managing companies")
@RequestMapping("/api/companies")
@AllArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    private final SpringSecurityAuditorAware springSecurityAuditorAware;

    @ApiOperation(value = "Api for return list of companies")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_READ', 'COMPANY_SAVE', 'COMPANY_DELETE', 'COMPANY_CREATE')")
    public Flux<CompanyDto> findAll(@ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        log.debug("Hello({}) is authenticated? ({})", authentication.getName(), authentication.isAuthenticated());
        if (hasRoleAdmin(authentication)) {
            return companyService.findAllActiveCompanies();
        } else {
            return companyService.findActiveCompaniesByUser(authentication.getName());
        }
    }

    @ApiOperation(value = "Api for return a company by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_READ', 'COMPANY_SAVE')")
    public Mono<CompanyDto> findById(@ApiParam(required = true) @PathVariable String id,
                                    @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        log.debug("Hello({}) is authenticated? ({})", authentication.getName(), authentication.isAuthenticated());
        return companyService.findById(id)
            .flatMap(p -> {
                if (hasRoleAdmin(authentication) || p.getCreatedByUser().equals(authentication.getName())) {
                    return Mono.just(p);
                } else {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("User(%s) does not have access to this resource", authentication.getName())));
                }
            })
            .switchIfEmpty(responseNotFound());
    }

    @ApiOperation(value = "Api for creating a company")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_CREATE')")
    public Mono<ResponseEntity<CompanyDto>> create(@RequestBody @ApiParam(required = true) @Valid CompanyDto companyDto,
                                                  @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        springSecurityAuditorAware.setCurrentAuthenticatedUser(authentication);
        return companyService.save(companyDto)
                .map(p -> ResponseEntity.created(URI.create(String.format("/api/companies/%s", p.getId())))
                        .body(p));
    }

    @ApiOperation(value = "Api for updating a company")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_SAVE')")
    public Mono<CompanyDto> update(@RequestBody @ApiParam(required = true) @Valid CompanyDto companyDto,
                                  @PathVariable @ApiParam(required = true) String id,
                                  @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        springSecurityAuditorAware.setCurrentAuthenticatedUser(authentication);
        companyDto.setId(id);
        return companyService.findById(id)
                .switchIfEmpty(responseNotFound())
                .flatMap(p -> companyService.save(companyDto));
    }

    @ApiOperation(value = "Api for deleting a company")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_DELETE')")
    public Mono<Void> delete(@PathVariable @ApiParam(required = true) String id,
                             @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        return companyService.findById(id)
            .switchIfEmpty(responseNotFound())
            .flatMap(u -> {
                if (hasRoleAdmin(authentication) || u.getCreatedByUser().equals(authentication.getName())) {
                    return companyService.deleteById(id);
                } else {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("User(%s) does not have access to delete this resource", authentication.getName())));
                }
            });
    }

    private boolean hasRoleAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private Mono<CompanyDto> responseNotFound() {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

}
