# 🎯 TESTING GUIDE - ChatSpringAI RAG System

## 📁 Fichiers de Test Disponibles

| Fichier | Description | Type |
|---------|-------------|------|
| `postman_collection.json` | Collection complète Postman | GUI |
| `POSTMAN_GUIDE.md` | Guide détaillé Postman | 📖 Doc |
| `POSTMAN_QUICK_START.md` | Guide rapide 5 min | ⚡ Quick |
| `test_rag.sh` | Script test (Bash/Linux/Mac) | 🐧 Shell |
| `test_rag.ps1` | Script test (PowerShell/Windows) | 🪟 Script |
| `test_rag_document.txt` | Document TXT pour test RAG | 📄 Data |

---

## 🚀 OPTION 1: Postman (Recommandé pour GUI)

### ✅ Installation
1. Télécharger Postman: https://www.postman.com/downloads/
2. Installer et ouvrir Postman
3. **File → Import**
4. Sélectionner `postman_collection.json`

### ✅ Test Rapide (5 min)
- Lire: `POSTMAN_QUICK_START.md`
- Suivre les 7 étapes
- ✅ RAG fonctionne!

### ✅ Test Détaillé
- Lire: `POSTMAN_GUIDE.md`
- Comprendre chaque endpoint
- Tester cas avancés

---

## 🖥️ OPTION 2: Terminal PowerShell (Windows)

### ✅ Exécution
```powershell
cd C:\Users\axelp\Projects\chatspringai
.\test_rag.ps1
```

### ✅ Résultat Attendu
```
🚀 ChatSpringAI RAG - Test Suite
Base URL: http://localhost:8080
Conversation ID: 550e8400-e29b-41d4-a716-446655440000

1️⃣  Testing Health Check...
{ "status": "UP" }
✅ Server is running

2️⃣  Uploading Document...
{ "success": true, "documentId": "...", "numberOfChunks": 3 }
✅ Document uploaded
📊 Chunks created: 3

3️⃣  First Chat Message (RAG Enabled)...
{ "success": true, "content": "Les points clés du RAG sont..." }

4️⃣  Second Chat Message...
{ "success": true, "content": "La vectorisation est..." }

5️⃣  Fetching Conversation History...
[ { "role": "USER", "content": "..." }, { "role": "ASSISTANT", ... } ]

6️⃣  Deleting Document...
{ "success": true, "message": "Document supprimé" }

✅ All tests completed!
```

---

## 🐧 OPTION 3: Terminal Bash (Linux/Mac)

### ✅ Exécution
```bash
cd ~/chatspringai
bash test_rag.sh
```

### ✅ Même résultat qu'en PowerShell

---

## 🔍 ENDPOINTS TESTÉS

### 1. **Health Check** ✅
```
GET /actuator/health
Status: 200
Response: { "status": "UP" }
```

### 2. **Upload Document** ✅
```
POST /api/documents/upload
Params: conversationId, file
Response: { documentId, numberOfChunks, success }
```

### 3. **Send Chat (RAG)** ✅
```
POST /api/chat/send
Body: { conversationId, userMessage }
Response: { success, content, timestamp }
```

### 4. **Get History** ✅
```
GET /api/chat/history/:conversationId
Response: [ { role, content } ]
```

### 5. **Delete Document** ✅
```
DELETE /api/documents/:documentId
Response: { success, message }
```

---

## 📊 CE QUI SE PASSE EN ARRIÈRE-PLAN

```
UPLOAD:
  test_rag_document.txt
    ↓
  DocumentProcessingService (Parse)
    ↓
  TokenTextSplitter (Chunk) → 3 chunks
    ↓
  EmbeddingModel (Vectorize) → nomic-embed-text
    ↓
  PostgreSQL + pgvector (Store)
    ↓
  ✅ Ready for RAG

CHAT (avec RAG):
  User Question
    ↓
  EmbeddingModel (Vectorize question)
    ↓
  VectorStore.similaritySearch() (Find relevant chunks)
    ↓
  Top-5 chunks retrieved + metadata
    ↓
  Augment system prompt with context
    ↓
  Ollama (Generate response)
    ↓
  ✅ Response based on document!
```

---

## 🎯 SCÉNARIOS DE TEST

### Test 1: Baseline (No RAG)
```
1. Ne pas uploader de document
2. Poser une question générale
3. Réponse du modèle sans contexte
```

### Test 2: Avec RAG (1 Document)
```
1. Uploader test_rag_document.txt
2. Poser des questions spécifiques
3. Réponse incluant le contenu du doc
```

### Test 3: Multi-Questions
```
1. Uploader document
2. Poser question 1 → Réponse 1
3. Poser question 2 → Réponse 2
4. Consulter historique
5. Voir conversation complète
```

### Test 4: Vrai Monde
```
1. Créer votre propre PDF
2. Uploader votre doc
3. Tester avec questions réelles
4. Observer la spécialisation
```

---

## ✨ RÉSULTATS ATTENDUS

### ✅ Succès
- ✓ Document uploadé avec chunks créés
- ✓ Questions reçoivent des réponses basées sur le document
- ✓ Historique conversation sauvegardé
- ✓ Document peut être supprimé

### ⚠️ Avertissements Normaux
- Warnings Hibernate sur dialect
- Warnings spring.jpa.open-in-view
- Logs verbeux (normal)

### ❌ Erreurs à Investiguer
- 500 Internal Server: Vérifier Spring logs
- 400 Bad Request: Vérifier paramètres
- 404 Not Found: Vérifier URL/IDs

---

## 🔧 TROUBLESHOOTING

### Spring ne démarre pas
```powershell
# Vérifier Java
java -version

# Relancer
cd C:\Users\axelp\Projects\chatspringai
mvn spring-boot:run -DskipTests
```

### Port 8080 déjà utilisé
```powershell
# Trouver le process
Get-Process | Where-Object {$_.ProcessName -like "*java*"}

# Tuer le process
Stop-Process -Id <PID> -Force
```

### PostgreSQL pas accessible
```powershell
# Vérifier container Docker
docker ps | Select-String "postgres"

# Relancer si nécessaire
docker start postgres-chatbot-rag
```

### Fichier test pas trouvé
```
Chercher le fichier dans:
C:\Users\axelp\Projects\chatspringai\test_rag_document.txt
```

---

## 📈 MÉTRIQUES DE SUCCÈS

Si tous les tests passent ✅:

| Métrique | Valeur |
|----------|--------|
| Upload Success Rate | 100% |
| Chunks Created | >0 |
| RAG Search Time | <1s |
| LLM Response Time | <5s |
| Total Latency | <6s |
| Document Deletion | 100% |

---

## 🎓 PROCHAINES ÉTAPES

1. **Intégration Frontend**: Ajouter upload widget dans Vue
2. **Production**: Déployer avec véritables documents
3. **Analytics**: Tracker performance RAG
4. **Multi-LLM**: Tester avec différents modèles

---

## 📞 SUPPORT

En cas de problème:
1. Vérifier PreRequis (Spring, DB, Ollama)
2. Consulter les guides (POSTMAN_GUIDE.md)
3. Vérifier les logs Spring
4. Tester avec curl directement

---

**Bon test! 🚀**
