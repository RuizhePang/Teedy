package com.sismics.docs.rest.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sismics.docs.core.dao.RegisterRequestDao;
import com.sismics.docs.core.model.jpa.RegisterRequest;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.ValidationUtil;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Register Request Resource
 *
 * @author Ruizhe PANG
 */
@Path("/registerRequest")
public class RegisterRequestResource extends BaseResource{

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(RegisterRequestResource.class);

    /**
     * Create a register request
     *
     * @param username the username
     * @param email the email
     * @param password the password
     * @return the response
     */
    @POST
    @Path("/register")
    public Response createRegisterRequest(
        @FormParam("username") String username,
        @FormParam("email") String email,
        @FormParam("password") String password) {

        username = ValidationUtil.validateLength(username, "username", 3, 50);
        ValidationUtil.validateUsername(username, "username");
        password = ValidationUtil.validateLength(password, "password", 8, 50);
        email = ValidationUtil.validateLength(email, "email", 1, 100);
        ValidationUtil.validateEmail(email, "email");

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        RegisterRequestDao registerRequestDao = new RegisterRequestDao();
        try {
            registerRequestDao.createRegisterRequest(registerRequest);
        } catch (Exception e) {
            if("AlreadyExistingUsername".equals(e.getMessage())) {
                throw new ClientException("AlreadyExistingUsername", "Login already used", e);
            } else {
                throw new ServerException("UnknownError", "Unknown server error", e);
            }
        }

        JsonObjectBuilder response = Json.createObjectBuilder()
            .add("status", "ok");

        return Response.ok().entity(response.build()).build();
    }
}
