# Contribuer à Sillage

Merci de contribuer à Sillage. Une modification doit rester ciblée, testée et documentée.

## Développement local

1. Créez une branche depuis `main`.
2. Installez les dépendances frontend avec `npm ci` dans `front/`.
3. Lancez PostgreSQL avec `docker compose up -d postgres`.
4. Vérifiez vos changements avant de proposer une pull request.

```powershell
.\mvnw.cmd test
Set-Location front
npm run lint
npm run build
```

## Pull requests

- expliquez le problème résolu et la solution retenue ;
- ajoutez ou adaptez les tests lorsque le comportement change ;
- évitez de mélanger refactor, formatage massif et nouvelle fonctionnalité ;
- ne versionnez jamais de secrets, documents privés ou modèles Ollama.

## Conventions

- Java 21 et injection par constructeur côté backend ;
- Composition API et composants ciblés côté Vue ;
- messages d’erreur utilisateur en français, logs techniques sans données sensibles ;
- Conventional Commits recommandés (`feat:`, `fix:`, `docs:`, `refactor:`).
