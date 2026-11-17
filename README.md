# A2A + MCP Demo — Java 21 / Spring Boot

Démonstration d'une architecture multi-agents combinant deux protocoles ouverts :

- **MCP (Model Context Protocol)** — pour connecter un agent à un outil externe (ici, une base PostgreSQL)
- **A2A (Agent-to-Agent)** — pour qu'un agent orchestrateur délègue à un agent spécialisé

Le tout en Java 21 / Spring Boot 3.4 / Spring AI 1.0, dockerisé, prêt à lancer en une commande.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Hôte                                                                       │
│                                                                             │
│  curl POST /chat ──────────────────────────────────────────────────────┐   │
│                                                                         │   │
└─────────────────────────────────────────────────────────────────────── │ ──┘
                                                                          │
                                           ╔══════════════════════════════╧════╗
                                           ║  agent-orchestrateur  :8080       ║
                                           ║  Spring AI + Claude               ║
                                           ║                                   ║
                                           ║  POST /chat                       ║
                                           ║    → ChatClient (Claude)          ║
                                           ║        décide d'appeler l'outil   ║
                                           ║        @Tool verifierFournisseur  ║
                                           ╚══════════════════╤════════════════╝
                                                              │
                                                   A2A  JSON-RPC 2.0
                                                   POST /  tasks/send
                                                              │
                                           ╔══════════════════╧════════════════╗
                                           ║  agent-metier  :8081              ║
                                           ║  Spring AI + Claude               ║
                                           ║                                   ║
                                           ║  GET  /.well-known/agent.json     ║
                                           ║  POST /  (tasks/send)             ║
                                           ║    → ChatClient (Claude)          ║
                                           ║        appelle l'outil MCP        ║
                                           ╚══════════════════╤════════════════╝
                                                              │
                                                   MCP  SSE Transport
                                                   rechercherFournisseur(nom)
                                                              │
                                           ╔══════════════════╧════════════════╗
                                           ║  mcp-fournisseurs  :8080          ║
                                           ║  Spring AI MCP Server             ║
                                           ║                                   ║
                                           ║  @Tool rechercherFournisseur      ║
                                           ║    SELECT … WHERE nom ILIKE …     ║
                                           ╚══════════════════╤════════════════╝
                                                              │
                                                   JDBC (user mcp_reader,
                                                         SELECT seulement)
                                                              │
                                           ╔══════════════════╧════════════════╗
                                           ║  postgres  :5432                  ║
                                           ║  DB : fournisseurs                ║
                                           ║  Table : fournisseur              ║
                                           ║  (id, nom, siret, ville)          ║
                                           ╚═══════════════════════════════════╝
```

### Flux de données détaillé

```
Utilisateur
    │
    │  POST /chat  {"message": "Est-ce que Dupont SAS est référencé?"}
    ▼
agent-orchestrateur
    │  Claude reçoit le message + la description de l'outil verifierFournisseur
    │  Claude décide d'appeler l'outil
    │
    │  @Tool verifierFournisseur("Est-ce que Dupont SAS est référencé?")
    │        ↳ construit une requête JSON-RPC A2A tasks/send
    ▼
agent-metier  (protocole A2A)
    │  POST /
    │  { "jsonrpc":"2.0", "method":"tasks/send",
    │    "params": { "id":"...", "message": {"role":"user","parts":[{"type":"text","text":"..."}]} } }
    │
    │  Claude reçoit la question + l'outil rechercherFournisseur (via MCP)
    │  Claude appelle l'outil MCP
    ▼
mcp-fournisseurs  (protocole MCP / SSE)
    │  rechercherFournisseur("Dupont SAS")
    │        ↳ SELECT * FROM fournisseur WHERE nom ILIKE '%Dupont SAS%'
    ▼
PostgreSQL
    │  Résultat : ID:1 | Dupont SAS | 12345678901234 | Paris
    ▲
    │
mcp-fournisseurs  → retourne le résultat formaté à l'agent-metier
    ▲
    │
agent-metier  → Claude formule la réponse en français
              → encapsule dans A2A artifact { "type":"text", "text":"..." }
              → retourne à l'agent-orchestrateur
    ▲
    │
agent-orchestrateur  → Claude formule la réponse finale
    ▲
    │
Utilisateur  ← {"response": "Oui, Dupont SAS est bien référencé. Siège à Paris, SIRET 12345678901234."}
```

---

## Services

| Service              | Port interne | Port hôte | Rôle                                      |
|----------------------|:------------:|:---------:|-------------------------------------------|
| `postgres`           | 5432         | —         | Base PostgreSQL, table `fournisseur`      |
| `mcp-fournisseurs`   | 8080         | —         | Serveur MCP, outil `rechercherFournisseur`|
| `agent-metier`       | 8081         | —         | Agent Claude, client MCP, serveur A2A     |
| `agent-orchestrateur`| 8080         | **8080**  | Agent Claude, client A2A, API REST        |

Seul l'orchestrateur est accessible depuis l'hôte. Les trois autres services communiquent sur le réseau Docker interne `demo-net`.

---

## Prérequis

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (ou Docker Engine + Compose)
- Une clé API Anthropic

---

## Démarrage

```bash
# 1. Cloner / se placer dans le répertoire
cd a2a-mcp-demo

# 2. Configurer les variables d'environnement
cp .env.example .env
# Éditer .env et renseigner ANTHROPIC_API_KEY

# 3. Lancer tous les services
docker compose up --build
```

L'ordre de démarrage est géré automatiquement via les healthchecks :
`postgres` → `mcp-fournisseurs` → `agent-metier` → `agent-orchestrateur`

Le stack est prêt quand vous voyez `agent-orchestrateur` passer `healthy` (environ 2–3 min au premier build).

---

## Utilisation

### Vérifier un fournisseur existant

```bash
curl -s -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Est-ce que Dupont SAS est un fournisseur référencé?"}' | jq .
```

```json
{
  "response": "Oui, Dupont SAS est bien référencé dans notre base. Voici ses informations : ID 1, Ville : Paris, SIRET : 12345678901234."
}
```

### Recherche partielle

```bash
curl -s -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Y a-t-il un fournisseur Martin dans la base?"}' | jq .
```

### Fournisseur inexistant

```bash
curl -s -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Connaissez-vous la société Acme Corp?"}' | jq .
```

```json
{
  "response": "Non, Acme Corp n'est pas référencée dans notre base de données fournisseurs."
}
```

### Consulter l'Agent Card de l'agent métier

```bash
curl -s http://localhost:8081/.well-known/agent.json | jq .
```

> `agent-metier` n'est pas exposé sur l'hôte, mais vous pouvez l'appeler depuis un conteneur :
> ```bash
> docker compose exec agent-orchestrateur \
>   curl -s http://agent-metier:8081/.well-known/agent.json | jq .
> ```

---

## Structure du projet

```
a2a-mcp-demo/
├── .env.example
├── .gitignore
├── docker-compose.yml
│
├── postgres/
│   └── init.sql                        # Schéma + données + user mcp_reader
│
├── mcp-fournisseurs/                   # Serveur MCP
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/mcp/
│       ├── McpFournisseursApplication.java
│       ├── config/McpToolsConfig.java  # Enregistre l'outil via MethodToolCallbackProvider
│       ├── domain/Fournisseur.java     # Entité JPA
│       ├── repository/FournisseurRepository.java  # Requête ILIKE native
│       └── tool/FournisseurToolService.java       # @Tool rechercherFournisseur
│
├── agent-metier/                       # Agent Claude + serveur A2A
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/metier/
│       ├── AgentMetierApplication.java
│       ├── config/AgentConfig.java     # ChatClient avec outils MCP + prompt système
│       ├── service/AgentMetierService.java
│       └── a2a/
│           ├── A2AController.java      # GET /.well-known/agent.json + POST /
│           └── dto/                    # Records A2A (request, response, card…)
│
└── agent-orchestrateur/                # Agent Claude + API REST
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/example/orchestrateur/
        ├── AgentOrchestrateurApplication.java
        ├── config/OrchestratorConfig.java  # ChatClient avec outil A2A + prompt système
        ├── controller/ChatController.java  # POST /chat
        ├── controller/dto/                 # ChatRequest, ChatResponse
        ├── service/OrchestratorService.java
        └── tool/AgentMetierTool.java       # @Tool verifierFournisseur → appel A2A
```

---

## Technologies

| Couche        | Technologie                                  |
|---------------|----------------------------------------------|
| Langage       | Java 21                                      |
| Framework     | Spring Boot 3.4.1                            |
| IA            | Spring AI 1.0.0 + Anthropic Claude           |
| MCP Server    | `spring-ai-starter-mcp-server-webmvc` (SSE)  |
| MCP Client    | `spring-ai-starter-mcp-client`               |
| A2A           | Implémentation HTTP/JSON-RPC 2.0 manuelle    |
| Base de données | PostgreSQL 16                              |
| Conteneurs    | Docker Compose                               |

### Pourquoi MCP et A2A ensemble ?

**MCP** résout le problème de l'accès aux données : il permet à un LLM d'appeler des outils externes (bases de données, APIs, fichiers) de façon standardisée, sans que le code de l'agent ait besoin de connaître le détail des requêtes SQL.

**A2A** résout le problème de la collaboration entre agents : il permet à un agent orchestrateur de déléguer des tâches à des agents spécialisés, sans couplage fort. L'orchestrateur n'a pas besoin de savoir comment l'agent métier fonctionne en interne — il lui envoie un message et reçoit une réponse.

Combinés, ils permettent une séparation claire :
- L'orchestrateur ne connaît pas la base de données.
- L'agent métier ne connaît pas les requêtes SQL.
- Seul le serveur MCP parle directement à PostgreSQL, avec un utilisateur en lecture seule.

---

## Configuration

### Variables d'environnement (`.env`)

| Variable          | Obligatoire | Défaut               | Description               |
|-------------------|:-----------:|----------------------|---------------------------|
| `ANTHROPIC_API_KEY` | Oui       | —                    | Clé API Anthropic         |
| `ANTHROPIC_MODEL`   | Non        | `claude-sonnet-4-5`  | Modèle Claude à utiliser  |

### Changer le modèle

```bash
# Dans .env
ANTHROPIC_MODEL=claude-opus-4-5
```

---

## Commandes utiles

```bash
# Voir les logs en temps réel
docker compose logs -f

# Logs d'un service spécifique
docker compose logs -f agent-orchestrateur

# État des healthchecks
docker compose ps

# Arrêter et supprimer les conteneurs
docker compose down

# Arrêter et supprimer y compris les volumes
docker compose down -v
```
