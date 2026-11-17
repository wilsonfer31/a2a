package com.example.metier.a2a;

import com.example.metier.a2a.dto.A2AArtifact;
import com.example.metier.a2a.dto.A2APart;
import com.example.metier.a2a.dto.A2ARequest;
import com.example.metier.a2a.dto.A2AResponse;
import com.example.metier.a2a.dto.A2AResult;
import com.example.metier.a2a.dto.A2AStatus;
import com.example.metier.a2a.dto.AgentCapabilities;
import com.example.metier.a2a.dto.AgentCard;
import com.example.metier.a2a.dto.AgentSkill;
import com.example.metier.service.AgentMetierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class A2AController {

    private final AgentMetierService agentMetierService;

    public A2AController(AgentMetierService agentMetierService) {
        this.agentMetierService = agentMetierService;
    }

    @GetMapping("/.well-known/agent.json")
    public ResponseEntity<AgentCard> getAgentCard() {
        AgentCard card = new AgentCard(
                "Agent Métier Fournisseurs",
                "Agent de vérification de fournisseurs utilisant une base de données via MCP",
                "http://agent-metier:8081/",
                "1.0.0",
                new AgentCapabilities(false, false),
                List.of(
                        new AgentSkill(
                                "verifier-fournisseur",
                                "Vérifier un fournisseur",
                                "Recherche et vérifie l'existence d'un fournisseur dans la base de données",
                                List.of("text"),
                                List.of("text")
                        )
                )
        );
        return ResponseEntity.ok(card);
    }

    @PostMapping("/")
    public ResponseEntity<A2AResponse> handleTask(@RequestBody A2ARequest request) {
        if (!"tasks/send".equals(request.method())) {
            return ResponseEntity.badRequest().build();
        }

        String question = request.params().message().parts().stream()
                .filter(p -> "text".equals(p.type()))
                .map(A2APart::text)
                .collect(Collectors.joining(" "));

        String answer = agentMetierService.process(question);

        A2AResponse response = new A2AResponse(
                "2.0",
                new A2AResult(
                        request.params().id(),
                        new A2AStatus("completed"),
                        List.of(new A2AArtifact(List.of(new A2APart("text", answer))))
                ),
                request.id()
        );

        return ResponseEntity.ok(response);
    }
}
