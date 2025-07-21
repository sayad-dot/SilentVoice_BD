package com.example.silentvoice_bd.auth.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private String fullName;

    public AuthResponse(String token, String fullName) {
        this.token = token;
        this.fullName = fullName;
        this.tokenType = "Bearer";
    }
}
