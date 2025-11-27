package org.shiki.prattparserrestfulapi.helper;

public record ApiResponse<T>(T data, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<T>(data, "Success");
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<T>(null, message);
    }
}