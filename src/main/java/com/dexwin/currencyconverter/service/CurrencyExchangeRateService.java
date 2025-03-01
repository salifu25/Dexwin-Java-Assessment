package com.dexwin.currencyconverter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: Implementation of this class has to be backed by https://api.exchangerate.host/latest?base=EUR&symbols=AUD,CAD,CHF,CNY,GBP,JPY,USD
 */

@Service
public class CurrencyExchangeRateService implements CurrencyService {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyExchangeRateService.class);
    private static final String BASE_CURRENCY = "EUR";

    @Value("${exchange.api.key}")
    private String apiKey;

    @Value("${exchange.api.url}")
    private String API_URL;

    @Value("${exchange.api.currencies}")
    private String SUPPORTED_CURRENCIES;

    private final RestTemplate restTemplate;
    private final Map<String, Double> exchangeRates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API Key is missing. Set it in application.properties");
        }
        logger.info("API Key loaded successfully: {}", apiKey);
        fetchExchangeRates();
    }

    public CurrencyExchangeRateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public double convert(String source, String target, double amount) {
        if (source.equalsIgnoreCase(target)) {
            logger.info("Source and target currencies are the same ({}). Returning input amount: {}", source, amount);
            return amount;
        }

        Double sourceRate = getRate(source);
        Double targetRate = getRate(target);

        if (sourceRate == null || targetRate == null) {
            logger.error("Exchange rate unavailable for {} or {}", source, target);
            throw new IllegalArgumentException("Exchange rate unavailable for " + source + " or " + target);
        }

        double convertedAmount = Math.round((amount / sourceRate) * targetRate * 100.0) / 100.0; // Round to 2 decimal places
        logger.info("Converted {} {} -> {} {}", amount, source, convertedAmount, target);
        return convertedAmount;
    }

    private Double getRate(String currency) {
        if (BASE_CURRENCY.equalsIgnoreCase(currency)) {
            return 1.0;
        }

        Double rate = exchangeRates.get(currency.toUpperCase());
        if (rate == null) {
            throw new IllegalArgumentException("Invalid currency: " + currency);
        }

        return rate;
    }


    @Scheduled(fixedRate = 3600000) // Refresh rates hourly
    public void fetchExchangeRates() {
        String url = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("access_key", apiKey)
                .queryParam("source", BASE_CURRENCY)
                .queryParam("currencies", SUPPORTED_CURRENCIES)
                .toUriString();

        logger.info("Fetching exchange rates from API: {}", url);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                Object errorMessage = response != null ? response.get("error") : "Unknown error";
                logger.error("API request failed. Response: {}", errorMessage);
                throw new IllegalStateException("Failed to fetch exchange rates: " + errorMessage);
            }

            Map<String, Double> quotes = (Map<String, Double>) response.get("quotes");
            if (quotes == null || quotes.isEmpty()) {
                logger.error("No exchange rates found in API response.");
                return;
            }

            exchangeRates.clear();
            quotes.forEach((key, value) -> exchangeRates.put(key.substring(3), value)); // Remove "EUR" prefix

            logger.info("Exchange rates updated successfully.");
        } catch (Exception e) {
            logger.error("Error fetching exchange rates: {}", e.getMessage(), e);
        }
    }

}
