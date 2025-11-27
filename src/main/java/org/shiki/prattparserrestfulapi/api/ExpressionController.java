package org.shiki.prattparserrestfulapi.api;

import org.apfloat.Apfloat;
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
    public ApiResponse<String> evaluate(@RequestParam String expr) {
        Apfloat result = expressionService.evaluate(expr);
        return ApiResponse.ok(result.toString(true));
    }

    @PostMapping("/evaluate")
    public ApiResponse<String> evaluatePost(@RequestBody ExpressionRequest request) {
        Apfloat result = expressionService.evaluate(request.expr);
        return ApiResponse.ok(result.toString(true));
    }

    //Request DTO
    public record ExpressionRequest(String expr) {}
}
