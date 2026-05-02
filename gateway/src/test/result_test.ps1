param(
    [string]$RequestId = "fd9133fe-ee64-450a-b369-02b8ef9c5766"
)

$body = @"
{"query": "query { hashStatus(requestId: \u0022$RequestId\u0022) { status data } }"}
"@

Write-Host "Checking status for requestId: $RequestId"
$response = Invoke-RestMethod -Uri http://localhost:8080/graphql -Method Post -Body $body -ContentType "application/json"
$response.data.hashStatus