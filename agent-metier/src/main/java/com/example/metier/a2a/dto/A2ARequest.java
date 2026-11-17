package com.example.metier.a2a.dto;

public record A2ARequest(String jsonrpc, String method, A2AParams params, String id) {}
