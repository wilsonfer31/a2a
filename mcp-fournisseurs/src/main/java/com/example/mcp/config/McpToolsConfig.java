package com.example.mcp.config;

import com.example.mcp.tool.FournisseurToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider fournisseurTools(FournisseurToolService fournisseurToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(fournisseurToolService)
                .build();
    }
}
