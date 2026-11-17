package com.example.metier.a2a.dto;

import java.util.List;

public record AgentCard(
        String name,
        String description,
        String url,
        String version,
        AgentCapabilities capabilities,
        List<AgentSkill> skills
) {}
