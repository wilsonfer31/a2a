package com.example.mcp.tool;

import com.example.mcp.domain.Fournisseur;
import com.example.mcp.repository.FournisseurRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FournisseurToolService {

    private final FournisseurRepository fournisseurRepository;

    public FournisseurToolService(FournisseurRepository fournisseurRepository) {
        this.fournisseurRepository = fournisseurRepository;
    }

    @Tool(description = "Recherche des fournisseurs par nom dans la base de données (insensible à la casse)")
    public String rechercherFournisseur(String nom) {
        List<Fournisseur> results = fournisseurRepository.rechercherParNom(nom);

        if (results.isEmpty()) {
            return "Aucun fournisseur trouvé pour le nom: " + nom;
        }

        StringBuilder sb = new StringBuilder("Fournisseurs trouvés:\n");
        for (Fournisseur f : results) {
            sb.append("- ID: ").append(f.getId())
              .append(" | Nom: ").append(f.getNom())
              .append(" | SIRET: ").append(f.getSiret())
              .append(" | Ville: ").append(f.getVille())
              .append("\n");
        }
        return sb.toString();
    }
}
