package org.shiki.prattparserrestfulapi.api;

import org.shiki.prattparserrestfulapi.helper.ApiResponse;
import org.shiki.prattparserrestfulapi.service.ExpressionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/expression")
public class ExpressionController {

    private final ExpressionService expressionService;

    public ExpressionController(ExpressionService expressionService) {
        this.expressionService = expressionService;
    }

    @GetMapping("/evaluate")
    public ApiResponse<Double> evaluate(@RequestParam String expr) {
        double result = expressionService.evaluate(expr);
        return ApiResponse.ok(result);
    }

    @PostMapping("/evaluate")
    public ApiResponse<Double> evaluatePost(@RequestBody ExpressionRequest request) {
        double result = expressionService.evaluate(request.expr);
        return ApiResponse.ok(result);
    }

    //Request DTO
    public record ExpressionRequest(String expr) {}
}
