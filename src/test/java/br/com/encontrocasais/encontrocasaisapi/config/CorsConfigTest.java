package br.com.encontrocasais.encontrocasaisapi.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CorsConfigTest {

    @Test
    void handlesPreflightAtHighestPriorityForAllowedOrigin() throws Exception {
        var registration = new CorsConfig().corsFilter();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new Object())
                .addFilters(registration.getFilter())
                .build();

        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "https://samdevf.github.io")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://samdevf.github.io"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Headers", "content-type, authorization"));
    }
}
