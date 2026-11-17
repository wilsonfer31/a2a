package com.example.metier.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AgentMetierService {

    private final ChatClient chatClient;

    public AgentMetierService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String process(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
