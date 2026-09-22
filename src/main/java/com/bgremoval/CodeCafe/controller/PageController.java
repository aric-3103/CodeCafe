package com.bgremoval.CodeCafe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the single-page Thymeleaf template for the background removal tool.
 *
 * <p>Requirements: 10.1, 10.3
 */
@Controller
public class PageController {

    /**
     * Renders the main single-page application shell.
     *
     * @return logical Thymeleaf view name "index"
     */
    @GetMapping("/bg-removal")
    public String index() {
        return "index";
    }
}
