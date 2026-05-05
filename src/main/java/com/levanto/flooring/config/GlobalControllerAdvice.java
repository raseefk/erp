package com.levanto.flooring.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CompanyProperties companyProperties;

    @ModelAttribute("companyName")
    public String getCompanyName() {
        return companyProperties.getName();
    }

    @ModelAttribute("companyTagline")
    public String getCompanyTagline() {
        return companyProperties.getTagline();
    }
}
