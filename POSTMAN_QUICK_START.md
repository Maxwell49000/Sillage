# 🚀 Guide Visuel Postman - Test RAG en 5 minutes

## PRÉREQUIS
✅ Spring Boot tournant sur http://localhost:8080
✅ PostgreSQL pgvector en Docker
✅ Postman installé

---

## ÉTAPE 1: IMPORT DE LA COLLECTION

```
File → Import → postman_collection.json
```

Vous devriez voir dans le sidebar:
```
ChatSpringAI RAG System
├── Health Check
├── Setup Variables
├── Upload Document (TXT)
├── Upload Document (PDF Sample)
├── Send Chat Message with RAG
├── Ask About Document Content
├── Get Conversation History
└── Delete Document
```

---

## ÉTAPE 2: VÉRIFIER LA CONNEXION

1. **Cliquer**: "Health Check"
2. **Bouton**: Send
3. **Résultat**: Status 200 + `"status": "UP"`

```
✓ Si OK → Continuer
✗ Si erreur → Vérifier que Spring Boot tourne
```

---

## ÉTAPE 3: UPLOADER UN DOCUMENT

1. **Cliquer**: "Upload Document (TXT)"
2. **Body → Form Data**:
   - conversationId: `{{conversation_id}}` ← AUTO-REMPLI
   - file: Sélectionner `test_rag_document.txt`

```
📤 ENVOI:
---form-data---
conversationId: 550e8400-e29b-41d4...
file: test_rag_document.txt
--------
```

3. **Send**

```
📥 RÉPONSE (200 OK):
{
  "success": true,
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "test_rag_document.txt",
  "numberOfChunks": 3,
  "message": "Document uploadé et vectorisé avec succès"
}
```

**✓ À ce stade:**
- Le document a été parsé
- Découpé en 3 chunks
- Vectorisé et stocké dans pgvector
- Les variables Postman se remplissent automatiquement

---

## ÉTAPE 4: POSER UNE QUESTION (RAG ACTIVÉ)

1. **Cliquer**: "Send Chat Message with RAG"
2. **Body JSON**:
```json
{
  "conversationId": "{{conversation_id}}",
  "message": "Quels sont les points clés du RAG?"
}
```

3. **Send**

```
🔍 SYSTÈME EN ARRIÈRE-PLAN:
1. Vectorise la question
2. Cherche les chunks similaires dans pgvector
3. Récupère les 5 chunks les plus pertinents
4. Ajoute au prompt: 
   - Instruction système du modèle
   - CONTEXTE DES CHUNKS
   - Question de l'utilisateur
5. Envoie au modèle Ollama
6. Reçoit la réponse
7. Sauvegarde dans l'historique
```

```
📥 RÉPONSE (200 OK):
{
  "success": true,
  "content": "Les points clés du RAG sont...",
  "conversationId": "550e8400-e29b-41d4...",
  "timestamp": 1740000000
}
```

**Remarquez**: La réponse est basée sur le contenu du document! ✨

---

## ÉTAPE 5: POSER UNE 2E QUESTION

1. **Cliquer**: "Ask About Document Content"
2. **Modifier le Body** avec une nouvelle question:
```json
{
  "conversationId": "{{conversation_id}}",
  "message": "Comment fonctionne la vectorisation?"
}
```

3. **Send**

```
💬 Le modèle va:
- Utiliser l'historique précédent
- Chercher à nouveau les chunks pertinents
- Répondre en restant fidèle au document
```

---

## ÉTAPE 6: CONSULTER L'HISTORIQUE

1. **Cliquer**: "Get Conversation History"
2. La URL se remplit auto: `/api/chat/history/{{conversation_id}}`
3. **Send**

```
📥 RÉPONSE:
[
  {
    "role": "USER",
    "content": "Quels sont les points clés du RAG?"
  },
  {
    "role": "ASSISTANT",
    "content": "Les points clés du RAG sont..."
  },
  {
    "role": "USER",
    "content": "Comment fonctionne la vectorisation?"
  },
  {
    "role": "ASSISTANT",
    "content": "La vectorisation est le processus..."
  }
]
```

---

## ÉTAPE 7: NETTOYER (OPTIONNEL)

1. **Cliquer**: "Delete Document"
2. La variable `{{document_id}}` se remplit auto
3. **Send**

```
✓ Document supprimé de pgvector et de la BD
```

---

## 🎯 CE QUI S'EST PASSÉ

| Étape | Action | Backend | Résultat |
|-------|--------|---------|----------|
| 3 | Upload TXT | Parse → Chunk → Embed → Store | 3 chunks vectorisés |
| 4 | Question 1 | RAG Search → Context Inject → LLM | Réponse basée sur doc |
| 5 | Question 2 | RAG Search → Context Inject → LLM | Réponse basée sur doc |
| 6 | Historique | BD Query | Conversation sauvée |
| 7 | Delete | Cleanup | Doc supprimé |

---

## 🔥 TESTS AVANCÉS

### Test 1: Charger un Vrai PDF
```
Upload Document (PDF Sample)
→ Sélectionner un PDF réel (ex: documentation.pdf)
→ Observer numberOfChunks
→ Poser des questions spécifiques
```

### Test 2: Multi-Documents
```
Upload Document 1: Guide React
Upload Document 2: Guide Vue
→ Uploader 2 docs avec le MÊME conversationId
→ Poser une question générale
→ Le RAG cherche dans TOUS les documents
```

### Test 3: Sans RAG (Baseline)
```
Uploader juste une convo sans document
→ Le modèle répond sans contexte
→ Comparer avec réponses avec RAG
→ Observer la différence de qualité
```

### Test 4: Performance
```
Uploader un grand PDF (100+ pages)
→ Observer le temps d'ingestion
→ Mesurer le temps de réponse
→ Observer numberOfChunks
```

---

## ✨ RÉSUMÉ

✅ Vous pouvez uploader des documents
✅ Le RAG les vectorise automatiquement
✅ Le modèle répond basé sur le contexte
✅ La conversation est persistée
✅ Vous pouvez supprimer les documents

**Le modèle s'est SPÉCIALISÉ sur vos documents!** 🎓

---

## 🆘 PROBLÈMES COURANTS

| Erreur | Cause | Solution |
|--------|-------|----------|
| 500 Health | Spring pas lancé | `mvn spring-boot:run` |
| 400 Upload | Fichier invalid | Vérifier format TXT/PDF |
| Réponse vide | Doc pas trouvé | Vérifier numberOfChunks > 0 |
| 404 History | Convo non existante | Utiliser le conversation_id correct |

---

Démarrez le test! 🚀
