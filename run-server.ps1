param(
    [int]$Port = 5050,
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432,
    [string]$DbName = "postgres",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "123456",
    [string]$DbSslMode = "disable"
)

Write-Host ""
Write-Host "========================================"
Write-Host "        AUCTION SERVER STARTING"
Write-Host "========================================"
Write-Host ""

# =========================================================
# JavaFX module path (Mac Apple Silicon)
# =========================================================

$modulePath = @(
    "$HOME/.m2/repository/org/openjfx/javafx-base/21.0.6/javafx-base-21.0.6-mac-aarch64.jar",
    "$HOME/.m2/repository/org/openjfx/javafx-controls/21.0.6/javafx-controls-21.0.6-mac-aarch64.jar",
    "$HOME/.m2/repository/org/openjfx/javafx-graphics/21.0.6/javafx-graphics-21.0.6-mac-aarch64.jar",
    "$HOME/.m2/repository/org/openjfx/javafx-fxml/21.0.6/javafx-fxml-21.0.6-mac-aarch64.jar"
) -join ":"

# =========================================================
# PostgreSQL JDBC
# =========================================================

$postgresJar = "$HOME/.m2/repository/org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar"

# =========================================================
# Create target/classes
# =========================================================

if (-not (Test-Path "target/classes")) {
    New-Item -ItemType Directory -Path "target/classes" -Force | Out-Null
}

# =========================================================
# Generate source list
# =========================================================

if (Test-Path "sources.txt") {
    Remove-Item "sources.txt"
}

if (Test-Path "client/java") {
    Get-ChildItem "client/java" -Recurse -Filter *.java |
            ForEach-Object { $_.FullName } |
            Add-Content "sources.txt"
}

if (Test-Path "src/main/java") {
    Get-ChildItem "src/main/java" -Recurse -Filter *.java |
            ForEach-Object { $_.FullName } |
            Add-Content "sources.txt"
}

Write-Host "Compiling..."

# =========================================================
# Compile
# =========================================================

javac `
    --module-path="$modulePath" `
    --add-modules javafx.controls,javafx.fxml `
    -cp "$postgresJar" `
    -d "target/classes" `
    "@sources.txt"

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed."
    exit $LASTEXITCODE
}

# =========================================================
# Runtime classpath
# =========================================================

$classPath = "target/classes:$postgresJar"

Write-Host ""
Write-Host "Starting server..."
Write-Host ""

# =========================================================
# Run
# =========================================================

java `
    "-Dauction.server.port=$Port" `
    "-Dauction.db.host=$DbHost" `
    "-Dauction.db.port=$DbPort" `
    "-Dauction.db.name=$DbName" `
    "-Dauction.db.user=$DbUser" `
    "-Dauction.db.password=$DbPassword" `
    "-Dauction.db.sslmode=$DbSslMode" `
    --module-path="$modulePath" `
    --add-modules javafx.controls,javafx.fxml `
    -cp "$classPath" `
    server.ServerLauncherMain