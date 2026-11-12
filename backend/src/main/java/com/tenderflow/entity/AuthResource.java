package com.tenderflow.resource;

import com.tenderflow.entity.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    public static class RegisterRequest {
        public String fullName;
        public String email;
        public String password;
        public String role; // "BUYER" o "SUPPLIER"
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    @POST
    @Path("/register")
    @PermitAll
    @Transactional
    public Response register(RegisterRequest req) {
        if (req.email == null || req.password == null || req.role == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Compila tutti i campi obbligatori")).build();
        }

        if (User.findByEmail(req.email) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Email già registrata")).build();
        }

        String normalizedRole = req.role.toUpperCase();
        if (!normalizedRole.equals("BUYER") && !normalizedRole.equals("SUPPLIER")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Ruolo non valido. Usa BUYER o SUPPLIER")).build();
        }

        User user = new User();
        user.fullName = req.fullName != null ? req.fullName : req.email;
        user.email = req.email.toLowerCase().trim();
        user.password = BcryptUtil.bcryptHash(req.password);
        user.role = normalizedRole;
        user.persist();

        String token = generateToken(user);

        return Response.status(Response.Status.CREATED).entity(Map.of(
                "token", token,
                "email", user.email,
                "fullName", user.fullName,
                "role", user.role
        )).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(LoginRequest req) {
        if (req.email == null || req.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Inserisci email e password")).build();
        }

        User user = User.findByEmail(req.email.toLowerCase().trim());
        if (user == null || !BcryptUtil.matches(req.password, user.password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Credenziali non valide")).build();
        }

        String token = generateToken(user);

        return Response.ok(Map.of(
                "token", token,
                "email", user.email,
                "fullName", user.fullName,
                "role", user.role
        )).build();
    }

    private String generateToken(User user) {
        return Jwt.issuer("https://tenderflow.it/issuer")
                .upn(user.email)
                .groups(Set.of(user.role))
                .claim("fullName", user.fullName)
                .expiresIn(Duration.ofHours(24))
                .sign();
    }
}