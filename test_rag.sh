#!/bin/bash
# ============================================================
# ChatSpringAI RAG - Test Scripts (cURL)
# ============================================================

BASE_URL="http://localhost:8080"
CONV_ID=$(uuidgen)
DOC_ID=""

echo "🚀 ChatSpringAI RAG - Test Suite"
echo "Base URL: $BASE_URL"
echo "Conversation ID: $CONV_ID"
echo ""

# ============================================================
# 1. HEALTH CHECK
# ============================================================
echo "1️⃣  Testing Health Check..."
curl -s -X GET "$BASE_URL/actuator/health" | jq '.'
echo ""
echo "✅ Server is running\n"

# ============================================================
# 2. UPLOAD DOCUMENT
# ============================================================
echo "2️⃣  Uploading Document..."
UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "conversationId=$CONV_ID" \
  -F "file=@test_rag_document.txt")

echo "$UPLOAD_RESPONSE" | jq '.'
DOC_ID=$(echo "$UPLOAD_RESPONSE" | jq -r '.documentId')
NUM_CHUNKS=$(echo "$UPLOAD_RESPONSE" | jq -r '.numberOfChunks')

echo "✅ Document uploaded: $DOC_ID"
echo "📊 Chunks created: $NUM_CHUNKS\n"

# ============================================================
# 3. SEND FIRST CHAT MESSAGE (WITH RAG)
# ============================================================
echo "3️⃣  First Chat Message (RAG Enabled)..."
CHAT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/chat/send" \
  -H "Content-Type: application/json" \
  -d "{
    \"conversationId\": \"$CONV_ID\",
    \"userMessage\": \"Quels sont les points clés du RAG?\"
  }")

echo "$CHAT_RESPONSE" | jq '.'
echo ""

# ============================================================
# 4. SEND SECOND CHAT MESSAGE
# ============================================================
echo "4️⃣  Second Chat Message..."
CHAT_RESPONSE2=$(curl -s -X POST "$BASE_URL/api/chat/send" \
  -H "Content-Type: application/json" \
  -d "{
    \"conversationId\": \"$CONV_ID\",
    \"userMessage\": \"Comment fonctionne la vectorisation?\"
  }")

echo "$CHAT_RESPONSE2" | jq '.'
echo ""

# ============================================================
# 5. GET CONVERSATION HISTORY
# ============================================================
echo "5️⃣  Fetching Conversation History..."
curl -s -X GET "$BASE_URL/api/chat/history/$CONV_ID" | jq '.'
echo ""

# ============================================================
# 6. DELETE DOCUMENT
# ============================================================
echo "6️⃣  Deleting Document..."
curl -s -X DELETE "$BASE_URL/api/documents/$DOC_ID" | jq '.'
echo ""

echo "✅ All tests completed!"
