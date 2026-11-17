package com.example.mcp.repository;

import com.example.mcp.domain.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    @Query(value = "SELECT * FROM fournisseur WHERE nom ILIKE '%' || :nom || '%'", nativeQuery = true)
    List<Fournisseur> rechercherParNom(@Param("nom") String nom);
}
