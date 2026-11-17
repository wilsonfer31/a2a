package com.example.metier.a2a.dto;

import java.util.List;

public record AgentSkill(String id, String name, String description, List<String> inputModes, List<String> outputModes) {}
