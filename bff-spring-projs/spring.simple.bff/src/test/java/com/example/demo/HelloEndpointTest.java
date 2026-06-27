package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Integration tests for /hello endpoint.
 * This endpoint is publicly accessible (permitAll) but returns different content based on authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class HelloEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHello_Unauthenticated_Returns200WithEmptyBody() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void testHello_AsUser_Returns200WithUsername() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("user"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void testHello_AsAdmin_Returns200WithUsername() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    @WithMockUser(username = "customuser", roles = {"customrole"})
    void testHello_AsCustomUser_Returns200WithUsername() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("customuser"));
    }
}
