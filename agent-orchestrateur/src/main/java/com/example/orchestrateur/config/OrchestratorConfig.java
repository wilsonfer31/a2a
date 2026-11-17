package com.example.orchestrateur.config;

import com.example.orchestrateur.tool.AgentMetierTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrchestratorConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, AgentMetierTool agentMetierTool) {
        return builder
                .defaultSystem(
                    "Tu es un agent orchestrateur. " +
                    "Pour toute question concernant un fournisseur ou une entreprise référencée, " +
                    "tu utilises l'outil 'verifierFournisseur' pour déléguer à l'agent métier spécialisé. " +
                    "Ne réponds jamais toi-même aux questions sur les fournisseurs sans avoir utilisé cet outil."
                )
                .defaultTools(agentMetierTool)
                .build();
    }
}
