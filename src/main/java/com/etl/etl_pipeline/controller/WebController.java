package com.etl.etl_pipeline.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving web pages
 */
@Controller
public class WebController {

    /**
     * Serve the main application page
     * @return The name of the template to render
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
