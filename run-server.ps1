param(
    [int]$Port = 5050,
    [string]$DbHost = "",
    [int]$DbPort = 5432,
    [string]$DbName = "postgres",
    [string]$DbUser = "",
    [string]$DbPassword = "",
    [string]$DbSslMode = ""
)

$portProbe = [System.Net.Sockets.TcpClient]::new()
try {
    $connectTask = $portProbe.ConnectAsync("localhost", $Port)
    if ($connectTask.Wait(500)) {
        Write-Error "Cong $Port dang duoc su dung. Neu auction server da chay thi khong can mo them server. Neu day la tien trinh cu, hay dung tien trinh dang giu cong truoc."
        exit 1
    }
} catch {
    # Connection failures are expected when the port is free.
} finally {
    $portProbe.Dispose()
}

$modulePath = @(
    "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-base\21.0.6\javafx-base-21.0.6-win.jar",
    "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar",
    "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar",
    "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar"
) -join ';'
$postgresJar = "$env:USERPROFILE\.m2\repository\org\postgresql\postgresql\42.7.5\postgresql-42.7.5.jar"

if (-not (Test-Path target\classes)) {
    New-Item -ItemType Directory -Path target\classes | Out-Null
}

Get-ChildItem client\java -Recurse -Filter *.java |
    ForEach-Object { $_.FullName.Substring($PWD.Path.Length + 1) } |
    Set-Content sources.txt
Get-ChildItem src\main\java -Recurse -Filter *.java |
    ForEach-Object { $_.FullName.Substring($PWD.Path.Length + 1) } |
    Add-Content sources.txt

javac --module-path $modulePath --add-modules javafx.controls,javafx.fxml -d target\classes "@sources.txt"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not (Test-Path $postgresJar)) {
    Write-Error "Chua tim thay PostgreSQL JDBC driver tai $postgresJar. Can tai dependency org.postgresql:postgresql:42.7.5 truoc."
    exit 1
}

$classPath = "target\classes;$postgresJar"
$javaArgs = @(
    "-Dauction.server.port=$Port"
)

if ($DbHost -and $DbHost.Trim()) {
    $javaArgs += "-Dauction.db.host=$DbHost"
}
if ($DbPort) {
    $javaArgs += "-Dauction.db.port=$DbPort"
}
if ($DbName -and $DbName.Trim() -and $DbHost -and $DbHost.Trim()) {
    $javaArgs += "-Dauction.db.name=$DbName"
}
if ($DbUser -and $DbUser.Trim()) {
    $javaArgs += "-Dauction.db.user=$DbUser"
}
if ($DbPassword -and $DbPassword.Trim()) {
    $javaArgs += "-Dauction.db.password=$DbPassword"
}
if ($DbSslMode -and $DbSslMode.Trim()) {
    $javaArgs += "-Dauction.db.sslmode=$DbSslMode"
}

$javaArgs += @(
    "--module-path", $modulePath,
    "--add-modules", "javafx.controls,javafx.fxml",
    "-cp", $classPath,
    "server.ServerLauncherMain"
)

& java @javaArgs
