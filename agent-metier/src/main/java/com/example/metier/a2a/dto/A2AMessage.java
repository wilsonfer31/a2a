package com.example.metier.a2a.dto;

import java.util.List;

public record A2AMessage(String role, List<A2APart> parts) {}
