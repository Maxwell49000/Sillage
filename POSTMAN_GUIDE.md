# 📌 Guide de Test Postman - ChatSpringAI RAG

## 🚀 Installation

### 1. Télécharger Postman
- Allez sur https://www.postman.com/downloads/
- Téléchargez et installez Postman

### 2. Importer la Collection
- Ouvrez Postman
- Cliquez sur **File** → **Import**
- Sélectionnez le fichier `postman_collection.json` de ce projet
- La collection **ChatSpringAI RAG System** apparaît dans le sidebar

### 3. Configurer l'Environnement
- Vérifiez que `base_url` est défini sur `http://localhost:8080`
- Les variables `conversation_id` et `document_id` se remplissent automatiquement à chaque test

---

## 🧪 Scénario de Test Complet

### **Étape 1: Vérifier la santé du serveur**
1. Ouvrir la requête **"Health Check"**
2. Cliquer sur **Send**
3. Vérifier que le statut est **200** et la réponse contient `"status": "UP"`

```json
{
  "status": "UP"
}
```

---

### **Étape 2: Uploader un Document PDF/DOCX/TXT**

#### Préparation
Créer un fichier texte `test_rag_document.txt` avec du contenu:

```text
INTRODUCTION AU RAG

Retrieval Augmented Generation (RAG) est une technique révolutionnaire 
qui améliore les modèles d'IA en:

1. Récupérant des documents pertinents
2. Ajoutant le contexte au prompt
3. Générant des réponses plus précises

Applications:
- Support client
- Analyse documentaire
- Questions-réponses spécialisées
```

#### Télécharger
1. Ouvrir la requête **"Upload Document (TXT)"**
2. Dans **Form Data**, vérifier:
   - `conversationId`: `{{conversation_id}}` (remplit auto)
   - `file`: Sélectionner `test_rag_document.txt`
3. Cliquer **Send**

#### Réponse attendue (200 OK)
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "test_rag_document.txt",
  "fileSize": 1234,
  "fileType": "text/plain",
  "numberOfChunks": 2,
  "uploadedAt": "2026-02-19T14:45:30.123456",
  "success": true,
  "message": "Document uploadé et vectorisé avec succès"
}
```

**Le système a:**
- ✅ Parsé le fichier TXT
- ✅ Découpé en **2 chunks**
- ✅ Vectorisé chaque chunk avec `nomic-embed-text`
- ✅ Stocké dans PostgreSQL + pgvector

---

### **Étape 3: Poser une Question (RAG activé)**

1. Ouvrir la requête **"Send Chat Message with RAG"**
2. Le body contient:
```json
{
  "conversationId": "{{conversation_id}}",
  "message": "Quels sont les points clés du document que je viens d'uploader?"
}
```

3. Cliquer **Send**

#### Réponse attendue
```json
{
  "success": true,
  "content": "Les points clés du RAG sont:\n1. Récupération de documents pertinents\n2. Augmentation du prompt avec contexte\n3. Génération de réponses précises\n\nCela améliore la qualité des réponses en donnant au modèle du contexte fiable.",
  "conversationId": "unique-id-123",
  "timestamp": 1740000000
}
```

**Coulisses du système:**
1. 🔍 **RAG Search**: Cherche les chunks du PDF pertinents
2. 📎 **Context Injection**: Ajoute le contexte au prompt système
3. 💬 **LLM Generation**: Le modèle génère une réponse basée sur le document
4. 💾 **Persistence**: La réponse est sauvegardée dans l'historique

---

### **Étape 4: Conversation Multi-tour**

1. Ouvrir **"Ask About Document Content"**
2. Modifier le `message` avec une nouvelle question:
```json
{
  "conversationId": "{{conversation_id}}",
  "message": "Comment intégrer RAG dans une application?"
}
```
3. Cliquer **Send**

Le système va à nouveau:
- Chercher les chunks pertinents
- Augmenter le prompt avec le contexte
- Répondre en restant fidèle au document

---

### **Étape 5: Consulter l'Historique**

1. Ouvrir **"Get Conversation History"**
2. La variable `{{conversation_id}}` est automatiquement remplie
3. Cliquer **Send**

Réponse attendue:
```json
[
  {
    "role": "USER",
    "content": "Quels sont les points clés du document?"
  },
  {
    "role": "ASSISTANT",
    "content": "Les points clés du RAG sont..."
  },
  {
    "role": "USER",
    "content": "Comment intégrer RAG?"
  },
  {
    "role": "ASSISTANT",
    "content": "Pour intégrer RAG..."
  }
]
```

---

### **Étape 6: Supprimer le Document**

1. Ouvrir **"Delete Document"**
2. La variable `{{document_id}}` se remplit depuis l'upload
3. Cliquer **Send**

Réponse:
```json
{
  "success": true,
  "message": "Document supprimé avec succès"
}
```

---

## 🎯 Cas d'Usage Avancés

### **Tester avec un PDF Réel**

1. Créer `documentation.pdf` (ex: guide utilisateur)
2. Ouvrir **"Upload Document (PDF Sample)"**
3. Sélectionner le PDF
4. Uploader
5. Poser des questions spécifiques au contenu du PDF

### **Tester la Spécialisation du Modèle**

**Document 1**: Uploader un guide React
- Question: "Comment créer un composant?"
- Réponse: Issue du guide React

**Document 2**: Uploader un guide Vue
- Question: "Comment créer une composable?"
- Réponse: Issue du guide Vue

**Test**: Le modèle se spécialise sur chaque document! 🎓

### **Tester les Chunks**

1. Uploader un PDF de 50+ pages
2. Observer `numberOfChunks` (devrait être 20+)
3. Poser une question très spécifique
4. Le modèle trouve et retourne le chunk pertinent

---

## 📊 Endpoints Disponibles

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/actuator/health` | Vérifier que le serveur tourne |
| POST | `/api/documents/upload` | Uploader un document |
| POST | `/api/chat/send` | Envoyer un message (RAG activé) |
| GET | `/api/chat/history/:id` | Récupérer l'historique |
| DELETE | `/api/documents/:id` | Supprimer un document |

---

## ✨ Variables Postman Utiles

```
base_url = http://localhost:8080
conversation_id = Auto-généré (ne pas modifier)
document_id = Auto-défini après upload
```

---

## 🐛 Troubleshooting

### ❌ Erreur 500 sur Health Check
- Vérifier que Spring Boot tourne: `mvn spring-boot:run -DskipTests`
- Vérifier port 8080 est libre: `netstat -ano | findstr :8080`

### ❌ Erreur 400 sur Upload
- Vérifier que le fichier existe
- Vérifier le format du fichier (TXT, PDF, DOCX)
- Vérifier que `conversationId` n'est pas vide

### ❌ Réponse vide du Chat
- Peut être normal si le fichier n'est pas copié
- Vérifier les logs: voir `documentId` retourné

---

## 🚀 Prochaines Étapes

1. **Intégration Frontend**: Ajouter un widget d'upload dans Vue
2. **Multi-documents**: Uploader plusieurs PDFs dans une conversation
3. **Analytics**: Tracker quels chunks sont utilisés
4. **Feedback**: Scorer la pertinence du RAG

---

Bon test! 🎉
