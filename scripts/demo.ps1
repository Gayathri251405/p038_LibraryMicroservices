$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$logDirectory = Join-Path $env:TEMP ('library-demo-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
    $jdk = Get-ChildItem (Join-Path $env:ProgramFiles 'Java') -Directory -Filter 'jdk-*' `
        -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) { $javaHome = $jdk.FullName }
}
$java = if ($javaHome) { Join-Path $javaHome 'bin\java.exe' } else {
    (Get-Command java.exe -ErrorAction Stop).Source
}
$processIds = [System.Collections.Generic.List[int]]::new()
$createdBookIds = [System.Collections.Generic.List[long]]::new()
$adminToken = $null
$gatewayReady = $false
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null

function Start-Service {
    param([string]$Name, [string]$Module, [int]$Port, [string]$Profile)
    $jarPath = Join-Path $projectRoot "$Module\target\$Module-1.0.0.jar"
    if (-not (Test-Path $jarPath)) { throw "Missing application jar: $jarPath. Run mvn verify first." }
    $arguments = @('-Xms64m', '-Xmx512m', '-jar', "`"$jarPath`"")
    if ($Profile) { $arguments += "--spring.profiles.active=$Profile" }
    $arguments += "--server.port=$Port"
    $process = Start-Process -FilePath $java -ArgumentList $arguments `
        -WorkingDirectory (Join-Path $projectRoot $Module) `
        -RedirectStandardOutput (Join-Path $logDirectory "$Name.out.log") `
        -RedirectStandardError (Join-Path $logDirectory "$Name.err.log") -PassThru
    $processIds.Add($process.Id)
    Write-Host "$Name PID=$($process.Id)"
    return $process.Id
}

function Wait-ForHealth {
    param([string]$Name, [int]$Port, [int]$ProcessId)
    for ($attempt = 0; $attempt -lt 100; $attempt++) {
        if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
            $logPath = Join-Path $logDirectory "$Name.out.log"
            if (Test-Path $logPath) { Get-Content $logPath -Tail 40 }
            throw "$Name exited before becoming healthy. Logs: $logDirectory"
        }
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$Port/actuator/health" `
                -TimeoutSec 2 -UseBasicParsing
            if ([int]$response.StatusCode -eq 200) {
                Write-Output "$Name healthy"
                return
            }
        } catch { Start-Sleep -Seconds 1 }
    }
    throw "$Name did not become healthy. Logs: $logDirectory"
}

function Invoke-DemoRequest {
    param([string]$Method, [string]$Path, [string]$Token, [object]$Body)
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $request = @{
        Uri = "http://localhost:8080$Path"; Method = $Method; Headers = $headers
        UseBasicParsing = $true; TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json'
        $request.Body = ConvertTo-Json -InputObject $Body -Compress
    }
    try {
        $response = Invoke-WebRequest @request
        return @{ Status = [int]$response.StatusCode; Body = $response.Content
            Instance = $response.Headers['X-Service-Instance'] }
    } catch {
        if (-not $_.Exception.Response) { throw }
        $response = $_.Exception.Response
        $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
        return @{ Status = [int]$response.StatusCode; Body = $reader.ReadToEnd()
            Instance = $response.Headers['X-Service-Instance'] }
    }
}

function Assert-Status {
    param($Response, [int]$Expected, [string]$Scenario)
    if ($Response.Status -ne $Expected) {
        throw "$Scenario expected HTTP $Expected, received HTTP $($Response.Status): $($Response.Body)"
    }
    Write-Output "$Scenario : HTTP $Expected"
}

try {
    $ports = @(8761, 8080, 8081, 8082, 8083)
    $listeners = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $ports -contains $_.LocalPort }
    if ($listeners) {
        $busyPorts = ($listeners | Select-Object -ExpandProperty LocalPort -Unique) -join ', '
        throw "Demo ports are already in use: $busyPorts"
    }

    Write-Output 'Starting Eureka, Auth, two Book Service instances, and Gateway...'
    $processId = Start-Service 'discovery-server' 'discovery-server' 8761 ''
    Wait-ForHealth 'discovery-server' 8761 $processId
    $processId = Start-Service 'auth-service' 'auth-service' 8081 'local'
    Wait-ForHealth 'auth-service' 8081 $processId
    $processId = Start-Service 'book-service-1' 'book-service' 8082 'local'
    Wait-ForHealth 'book-service-1' 8082 $processId
    $processId = Start-Service 'book-service-2' 'book-service' 8083 'local'
    Wait-ForHealth 'book-service-2' 8083 $processId
    $processId = Start-Service 'api-gateway' 'api-gateway' 8080 ''
    Wait-ForHealth 'api-gateway' 8080 $processId
    $gatewayReady = $true

    Assert-Status (Invoke-DemoRequest 'GET' '/api/books' '' $null) 401 'Missing token'
    Assert-Status (Invoke-DemoRequest 'GET' '/api/books' 'invalid.token.value' $null) 401 'Invalid token'

    $runId = [guid]::NewGuid().ToString('N').Substring(0, 12)
    $username = 'demo-reader-' + $runId
    $credentials = @{ username = $username; password = 'reader-password-123' }
    $registration = Invoke-DemoRequest 'POST' '/api/auth/register' '' $credentials
    Assert-Status $registration 200 'USER registration'
    $login = Invoke-DemoRequest 'POST' '/api/auth/login' '' $credentials
    Assert-Status $login 200 'USER login'
    $userToken = ($login.Body | ConvertFrom-Json).accessToken
    $denied = Invoke-DemoRequest 'POST' '/api/books' $userToken @{
        title = 'Forbidden'; author = 'Reader'
        isbn = 'DENIED-' + $runId; publishedYear = 2025
    }
    Assert-Status $denied 403 'USER write restriction'

    $adminUsername = if ($env:ADMIN_USERNAME) { $env:ADMIN_USERNAME } else { 'admin' }
    $adminPassword = if ($env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD } else { 'Admin12345!' }
    $adminLogin = Invoke-DemoRequest 'POST' '/api/auth/login' '' @{
        username = $adminUsername; password = $adminPassword
    }
    Assert-Status $adminLogin 200 'ADMIN login'
    $adminToken = ($adminLogin.Body | ConvertFrom-Json).accessToken
    $isbn = 'ADMIN-' + $runId
    $adminCreated = Invoke-DemoRequest 'POST' '/api/books' $adminToken @{
        title = 'Distributed Systems'; author = 'A. Writer'; isbn = $isbn; publishedYear = 2025
    }
    Assert-Status $adminCreated 201 'ADMIN create'
    $adminBook = $adminCreated.Body | ConvertFrom-Json
    $createdBookIds.Add([long]$adminBook.id)
    Assert-Status (Invoke-DemoRequest 'GET' "/api/books/$($adminBook.id)" $userToken $null) 200 'USER read'
    Assert-Status (Invoke-DemoRequest 'PUT' "/api/books/$($adminBook.id)" $adminToken @{
        title = 'Distributed Systems (Updated)'; author = 'A. Writer'
        isbn = $isbn; publishedYear = 2026
    }) 200 'ADMIN update'

    $librarianUsername = if ($env:LIBRARIAN_USERNAME) { $env:LIBRARIAN_USERNAME } else { 'librarian' }
    $librarianPassword = if ($env:LIBRARIAN_PASSWORD) { $env:LIBRARIAN_PASSWORD } else { 'Librarian123!' }
    $librarianLogin = Invoke-DemoRequest 'POST' '/api/auth/login' '' @{
        username = $librarianUsername; password = $librarianPassword
    }
    Assert-Status $librarianLogin 200 'LIBRARIAN login'
    $librarianToken = ($librarianLogin.Body | ConvertFrom-Json).accessToken
    $librarianIsbn = 'LIB-' + $runId
    $librarianCreated = Invoke-DemoRequest 'POST' '/api/books' $librarianToken @{
        title = 'Library Operations'; author = 'L. Keeper'
        isbn = $librarianIsbn; publishedYear = 2024
    }
    Assert-Status $librarianCreated 201 'LIBRARIAN create'
    $librarianBook = $librarianCreated.Body | ConvertFrom-Json
    $createdBookIds.Add([long]$librarianBook.id)
    Assert-Status (Invoke-DemoRequest 'PUT' "/api/books/$($librarianBook.id)" $librarianToken @{
        title = 'Library Operations (Updated)'; author = 'L. Keeper'
        isbn = $librarianIsbn; publishedYear = 2025
    }) 200 'LIBRARIAN update'
    Assert-Status (Invoke-DemoRequest 'DELETE' "/api/books/$($librarianBook.id)" $librarianToken $null) 204 'LIBRARIAN delete'
    [void]$createdBookIds.Remove([long]$librarianBook.id)

    $instances = [System.Collections.Generic.HashSet[string]]::new()
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        $response = Invoke-DemoRequest 'GET' '/api/books' $userToken $null
        Assert-Status $response 200 'Load-balanced read'
        if ($response.Instance) { [void]$instances.Add([string]$response.Instance) }
    }
    $instanceList = [string]::Join(', ', [string[]]$instances)
    Write-Output "Load-balanced instances observed: $instanceList"
    if ($instances.Count -lt 2) { throw 'Both Book Service instances were not observed through the gateway' }

    Assert-Status (Invoke-DemoRequest 'DELETE' "/api/books/$($adminBook.id)" $adminToken $null) 204 'ADMIN delete'
    [void]$createdBookIds.Remove([long]$adminBook.id)
    Assert-Status (Invoke-DemoRequest 'GET' "/api/books/$($adminBook.id)" $userToken $null) 404 'Read deleted book'
    Write-Output 'DEMO RESULT: PASS'
} catch {
    Write-Output "DEMO RESULT: FAIL - $_"
    Write-Output "Service logs: $logDirectory"
    throw
} finally {
    if ($gatewayReady -and $adminToken) {
        foreach ($bookId in $createdBookIds) {
            try { [void](Invoke-DemoRequest 'DELETE' "/api/books/$bookId" $adminToken $null) }
            catch { Write-Warning "Could not clean up demo book $bookId" }
        }
    }
    foreach ($processId in $processIds) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}
