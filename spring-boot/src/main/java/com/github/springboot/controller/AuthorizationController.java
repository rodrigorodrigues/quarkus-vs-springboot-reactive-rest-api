package com.github.springboot.controller;

import java.security.KeyPair;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import javax.validation.Valid;

import com.github.springboot.dto.AuthorizationDto;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("auth")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthorizationController {

    private final KeyPair keyPair;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> authorize(@RequestBody @Valid AuthorizationDto authorizationDto) throws Exception {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .subject(authorizationDto.getUser())
            .expirationTime(Date.from(ZonedDateTime.now().plusMinutes(1).toInstant()))
            .issueTime(new Date())
            .notBeforeTime(new Date())
            .claim("authorities", authorizationDto.getRoles())
            .claim("scope", "read")
            .jwtID(UUID.randomUUID().toString())
            .issuer("jwt")
            .build();
        JWSSigner signer = new RSASSASigner(keyPair.getPrivate());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("kid", "test");
        jsonObject.put("alg", JWSAlgorithm.RS256.getName());
        jsonObject.put("typ", "JWT");
        SignedJWT signedJWT = new SignedJWT(JWSHeader.parse(jsonObject), jwtClaimsSet);
        signedJWT.sign(signer);
        return ResponseEntity.ok(Collections.singletonMap("token", "Bearer " + signedJWT.serialize()));
    }
}
