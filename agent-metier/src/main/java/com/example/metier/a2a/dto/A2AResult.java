package com.example.metier.a2a.dto;

import java.util.List;

public record A2AResult(String id, A2AStatus status, List<A2AArtifact> artifacts) {}
