package com.dexwin.currencyconverter.controller;

import org.assertj.core.matcher.AssertionMatcher;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Ensures @BeforeAll is non-static
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${exchange.api.key}")
    private String apiKey;

    @BeforeAll
    void checkApiKeyValidity() {
        ResponseEntity<Map> response = restTemplate.getForEntity("https://api.exchangerate.host/live?access_key=" + apiKey, Map.class);
        Map<String, Object> body = response.getBody();

        boolean isValidKey = body != null && !body.containsKey("error");

        Assumptions.assumeTrue(isValidKey, "Skipping tests: Invalid API Key.");
    }

    @Test
    public void should_convert_EUR_to_USD_with_rate_greater_than_1() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=EUR&target=USD&amount=1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        new AssertionMatcher<>() {
                            @Override
                            public void assertion(String value) throws AssertionError {
                                assertThat(parseDouble(value)).isGreaterThan(1.0);
                            }
                        })
                );
    }

    @Test
    public void should_convert_USD_to_EUR_with_rate_less_than_1() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=USD&target=EUR&amount=1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        new AssertionMatcher<>() {
                            @Override
                            public void assertion(String value) throws AssertionError {
                                assertThat(parseDouble(value)).isLessThan(1.0);
                            }
                        })
                );
    }

    @Test
    public void should_return_same_amount_when_source_and_target_are_same() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=USD&target=USD&amount=100"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        new AssertionMatcher<>() {
                            @Override
                            public void assertion(String value) throws AssertionError {
                                assertThat(parseDouble(value)).isEqualTo(100.0);
                            }
                        })
                );
    }

    @Test
    public void should_return_bad_request_for_invalid_currency() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=XYZ&target=USD&amount=10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_bad_request_for_negative_amount() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=EUR&target=USD&amount=-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_bad_request_when_parameters_are_missing() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=EUR&target=USD"))
                .andExpect(status().isBadRequest());
    }

}