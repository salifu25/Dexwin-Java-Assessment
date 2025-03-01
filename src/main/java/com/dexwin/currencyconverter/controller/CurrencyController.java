package com.dexwin.currencyconverter.controller;

import com.dexwin.currencyconverter.service.CurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class CurrencyController {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyController.class);
    private final CurrencyService currencyService;

    public CurrencyController(final CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping(value = "currencies/convert", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> convert(
            @RequestParam("source") String source,
            @RequestParam("target") String target,
            @RequestParam("amount") double amount) {

        logger.info("Received request to convert {} {} to {}", amount, source, target);

        if (source == null || target == null || source.isBlank() || target.isBlank()) {
            logger.warn("Invalid input: Source or target currency is missing");
            return ResponseEntity.badRequest().body("Source and target currencies must be provided.");
        }

        if (amount <= 0) {
            logger.warn("Invalid input: Amount must be greater than zero");
            return ResponseEntity.badRequest().body("Amount must be greater than zero.");
        }

        try {
            double result = currencyService.convert(source, target, amount);
            logger.info("Conversion successful: {} {} = {} {}", amount, source, result, target);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("Conversion error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
