package com.pesco.authentication.authTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pesco.authentication.models.Users;
import com.pesco.authentication.payloads.UserSignInRequest;
import com.pesco.authentication.payloads.UserSignUpRequest;
import com.pesco.authentication.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Optional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsersRepository userRepository;

    @BeforeEach
    public void setUp() {
        // Clear the database before each test to ensure test isolation
        // userRepository.deleteAll();
    }

    @Test
    public void testRegisterUser_Success() throws Exception {
        // Create payload data
        UserSignUpRequest validRequest = new UserSignUpRequest(
                "John",
                "Doe",
                "validUsername",
                "brantyluizdeigo@gmail.com",
                "validPassword",
                "Male",
                "1234567890");

        // When: Perform the registration request
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(validRequest)))
                .andExpect(status().isCreated()) // Assert status code
                .andExpect(content().string(
                        "Thanks for your interest in joining Artex network! To complete account verification, email has been sent to email address you provided."));

        // Then: Verify the user is created in the database
        Optional<Users> user = userRepository.findByUsername("validUsername");
        assertTrue(user.isPresent()); // Ensure the user was saved
        assertEquals("brantyluizdeigo@gmail.com", user.get().getEmail());
    }

    // Test Login endpoint

    @Test
    public void testLogin_Success() throws Exception {
        UserSignInRequest loginRequest = new UserSignInRequest();
        loginRequest.setUsername("validUsername");
        loginRequest.setPassword("validPassword");

        // When: Perform the login request
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(loginRequest)))
                .andExpect(status().isOk()) // Assert successful login
                .andExpect(jsonPath("$.jwt").exists()) // Ensure a JWT is returned
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.username").value("validUsername"));
    }

}
