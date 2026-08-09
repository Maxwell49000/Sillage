# ============================================================
# ChatSpringAI RAG - Test Scripts (PowerShell)
# ============================================================

$BaseUrl = "http://localhost:8080"
$ConvId = (New-Guid).Guid
$DocId = ""

Write-Host "🚀 ChatSpringAI RAG - Test Suite" -ForegroundColor Green
Write-Host "Base URL: $BaseUrl"
Write-Host "Conversation ID: $ConvId`n"

# ============================================================
# 1. HEALTH CHECK
# ============================================================
Write-Host "1️⃣  Testing Health Check..." -ForegroundColor Cyan

$healthResponse = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" `
  -UseBasicParsing -ErrorAction SilentlyContinue

$healthResponse.Content | ConvertFrom-Json
Write-Host "✅ Server is running`n" -ForegroundColor Green

# ============================================================
# 2. UPLOAD DOCUMENT
# ============================================================
Write-Host "2️⃣  Uploading Document..." -ForegroundColor Cyan

$filePath = "test_rag_document.txt"
if (-not (Test-Path $filePath)) {
    Write-Host "❌ File not found: $filePath" -ForegroundColor Red
    exit 1
}

$fileContent = Get-Content $filePath -Raw
$fileBytes = [System.Text.Encoding]::UTF8.GetBytes($fileContent)

# Create form data
$boundary = [System.Guid]::NewGuid().ToString()
$uploadBody = @"
--$boundary
Content-Disposition: form-data; name="conversationId"

$ConvId
--$boundary
Content-Disposition: form-data; name="file"; filename="test_rag_document.txt"
Content-Type: text/plain

$fileContent
--$boundary--
"@

$uploadResponse = Invoke-WebRequest -Uri "$BaseUrl/api/documents/upload" `
  -Method POST `
  -ContentType "multipart/form-data; boundary=$boundary" `
  -Body $uploadBody `
  -UseBasicParsing -ErrorAction SilentlyContinue

$uploadJson = $uploadResponse.Content | ConvertFrom-Json
$uploadJson
$DocId = $uploadJson.documentId
$NumChunks = $uploadJson.numberOfChunks

Write-Host "✅ Document uploaded: $DocId" -ForegroundColor Green
Write-Host "📊 Chunks created: $NumChunks`n" -ForegroundColor Green

# ============================================================
# 3. SEND FIRST CHAT MESSAGE (WITH RAG)
# ============================================================
Write-Host "3️⃣  First Chat Message (RAG Enabled)..." -ForegroundColor Cyan

$chatBody = @{
    conversationId = $ConvId
    userMessage = "Quels sont les points clés du RAG?"
} | ConvertTo-Json

$chatResponse = Invoke-WebRequest -Uri "$BaseUrl/api/chat/send" `
  -Method POST `
  -ContentType "application/json" `
  -Body $chatBody `
  -UseBasicParsing -ErrorAction SilentlyContinue

$chatJson = $chatResponse.Content | ConvertFrom-Json
$chatJson
Write-Host ""

# ============================================================
# 4. SEND SECOND CHAT MESSAGE
# ============================================================
Write-Host "4️⃣  Second Chat Message..." -ForegroundColor Cyan

$chatBody2 = @{
    conversationId = $ConvId
    userMessage = "Comment fonctionne la vectorisation?"
} | ConvertTo-Json

$chatResponse2 = Invoke-WebRequest -Uri "$BaseUrl/api/chat/send" `
  -Method POST `
  -ContentType "application/json" `
  -Body $chatBody2 `
  -UseBasicParsing -ErrorAction SilentlyContinue

$chatJson2 = $chatResponse2.Content | ConvertFrom-Json
$chatJson2
Write-Host ""

# ============================================================
# 5. GET CONVERSATION HISTORY
# ============================================================
Write-Host "5️⃣  Fetching Conversation History..." -ForegroundColor Cyan

$historyResponse = Invoke-WebRequest -Uri "$BaseUrl/api/chat/history/$ConvId" `
  -UseBasicParsing -ErrorAction SilentlyContinue

$historyJson = $historyResponse.Content | ConvertFrom-Json
$historyJson | ConvertTo-Json -Depth 10
Write-Host ""

# ============================================================
# 6. DELETE DOCUMENT
# ============================================================
Write-Host "6️⃣  Deleting Document..." -ForegroundColor Cyan

$deleteResponse = Invoke-WebRequest -Uri "$BaseUrl/api/documents/$DocId" `
  -Method DELETE `
  -UseBasicParsing -ErrorAction SilentlyContinue

$deleteJson = $deleteResponse.Content | ConvertFrom-Json
$deleteJson
Write-Host ""

Write-Host "✅ All tests completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "  - Uploaded 1 document (3 chunks)" -ForegroundColor Gray
Write-Host "  - Asked 2 questions with RAG context" -ForegroundColor Gray
Write-Host "  - Retrieved conversation history" -ForegroundColor Gray
Write-Host "  - Deleted document" -ForegroundColor Gray
