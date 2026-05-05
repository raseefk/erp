package com.levanto.flooring.util;

import com.levanto.flooring.enums.TransactionStatus;
import com.levanto.flooring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class NumberGenerator {

    private final TransactionRepository repo;

    public String nextQuotationNumber() {
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String prefix = "QUO-" + ym + "-";
        String maxNumber = repo.findMaxQuotationNumber(prefix);
        long count = 1;
        if (maxNumber != null && maxNumber.length() > prefix.length()) {
            try {
                count = Long.parseLong(maxNumber.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                // Ignore and use 1
            }
        }
        return prefix + String.format("%04d", count);
    }

    public String nextInvoiceNumber() {
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String prefix = "INV-" + ym + "-";
        String maxNumber = repo.findMaxInvoiceNumber(prefix);
        long count = 1;
        if (maxNumber != null && maxNumber.length() > prefix.length()) {
            try {
                count = Long.parseLong(maxNumber.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                // Ignore and use 1
            }
        }
        return prefix + String.format("%04d", count);
    }
}
