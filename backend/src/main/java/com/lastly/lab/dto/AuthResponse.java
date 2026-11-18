package com.lastly.lab.dto;
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {}
