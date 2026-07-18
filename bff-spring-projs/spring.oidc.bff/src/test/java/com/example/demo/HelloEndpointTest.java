package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for /hello endpoint
 *
 * Authorization rule: anyRequest().permitAll() - public endpoint
 * Controller: return user != null ? user.getFullName() : null
 * getFullName() reads the standard OIDC "name" claim from the ID token.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class HelloEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Unauthenticated request — controller receives null OidcUser → returns null body.
     */
    @Test
    void testHello_Unauthenticated_Returns200WithNullBody() throws Exception {
        mockMvc.perform(get("/hello"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    /**
     * Authenticated user with "name" claim → controller returns that full name as body.
     */
    @Test
    void testHello_Authenticated_Returns200WithFullName() throws Exception {
        mockMvc.perform(get("/hello")
                .with(oidcLogin()
                    .idToken(token -> token.claim("name", "John Doe"))))
            .andExpect(status().isOk())
            .andExpect(content().string("John Doe"));
    }

    /**
     * User with ROLE_myuser — role does not affect /hello; full name still returned.
     */
    @Test
    void testHello_AsMyuser_Returns200WithFullName() throws Exception {
        mockMvc.perform(get("/hello")
                .with(oidcLogin()
                    .idToken(token -> token.claim("name", "Test User"))
                    .authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
            .andExpect(status().isOk())
            .andExpect(content().string("Test User"));
    }

    /**
     * User with ROLE_myadmin — role does not affect /hello; full name still returned.
     */
    @Test
    void testHello_AsAdmin_Returns200WithFullName() throws Exception {
        mockMvc.perform(get("/hello")
                .with(oidcLogin()
                    .idToken(token -> token.claim("name", "Admin User"))
                    .authorities(new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isOk())
            .andExpect(content().string("Admin User"));
    }
}