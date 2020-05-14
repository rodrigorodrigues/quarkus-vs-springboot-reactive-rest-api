package com.github.quarkus;

import java.util.Collections;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.github.common.AuthorizationDto;
import com.github.common.TokenUtils;
import io.quarkus.arc.profile.IfBuildProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@IfBuildProfile("auth")
@Path("/api/auth")
@RequestScoped
public class AuthorizationResource {

    @ConfigProperty(name = "PRIVATE_KEY_PATH", defaultValue = "")
    String privateKeyPath;

    @ConfigProperty(name = "PUBLIC_KEY_PATH", defaultValue = "")
    String publicKeyPath;

	@POST
	@PermitAll
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response authorize(@Valid AuthorizationDto authorizationDto) throws Exception {
		String token = TokenUtils.getInstance(privateKeyPath, publicKeyPath).generateTokenString(authorizationDto);
		return Response.ok().entity(Collections.singletonMap("token", "Bearer " + token)).build();
	}

}
