package com.etl.etl_pipeline.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller for handling errors
 */
@Controller
public class CustomErrorController implements ErrorController {

    /**
     * Handle error requests
     * @param request HTTP request
     * @return Model and view for error page
     */
    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        
        // Get error status code
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String errorMessage = "An error occurred";
        
        if (statusCode != null) {
            if (statusCode == 404) {
                errorMessage = "Page not found";
            } else if (statusCode == 500) {
                errorMessage = "Internal server error";
            }
        }
        
        modelAndView.addObject("errorCode", statusCode);
        modelAndView.addObject("errorMessage", errorMessage);
        
        return modelAndView;
    }
}
