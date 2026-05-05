package com.levanto.flooring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;

    public static <T> ApiResponse<T> ok(String msg, T data) { return new ApiResponse<>(true, msg, data); }
    public static <T> ApiResponse<T> ok(String msg)         { return new ApiResponse<>(true, msg, null); }
    public static <T> ApiResponse<T> error(String msg)      { return new ApiResponse<>(false, msg, null); }
}
