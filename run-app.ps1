param(
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5050,
    [string]$DbHost = "",
    [int]$DbPort = 5432,
    [string]$DbName = "postgres",
    [string]$DbUser = "",
    [string]$DbPassword = "",
    [string]$DbSslMode = "require"
)

# Tự động nhận diện chip Mac để lấy đúng thư viện JavaFX (-mac hoặc -mac-aarch64 cho chip M)
$architecture = (uname -m).Trim()
$osClassifier = if ($architecture -eq "arm64") { "mac-aarch64" } else { "mac" }

# Sửa thẳng phiên bản và đuôi file mà không cần dùng biến tự động nữa cho chắc chắn
$modulePath = @(
    "$env:HOME/.m2/repository/org/openjfx/javafx-base/21.0.6/javafx-base-21.0.6-mac-aarch64.jar",
    "$env:HOME/.m2/repository/org/openjfx/javafx-controls/21.0.6/javafx-controls-21.0.6-mac-aarch64.jar",
    "$env:HOME/.m2/repository/org/openjfx/javafx-graphics/21.0.6/javafx-graphics-21.0.6-mac-aarch64.jar",
    "$env:HOME/.m2/repository/org/openjfx/javafx-fxml/21.0.6/javafx-fxml-21.0.6-mac-aarch64.jar"
) -join ':'

$postgresJar = "$env:HOME/.m2/repository/org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar"

if (-not (Test-Path "target/classes")) {
    New-Item -ItemType Directory -Path "target/classes" | Out-Null
}

Get-ChildItem "client/java" -Recurse -Filter *.java |
        ForEach-Object { $_.FullName.Substring($PWD.Path.Length + 1) } |
        Set-Content sources.txt
Get-ChildItem "src/main/java" -Recurse -Filter *.java |
        ForEach-Object { $_.FullName.Substring($PWD.Path.Length + 1) } |
        Add-Content sources.txt

javac --module-path $modulePath --add-modules javafx.controls,javafx.fxml -d target/classes "@sources.txt"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Copy-Item "client/resources/*" "target/classes" -Recurse -Force
if (-not (Test-Path $postgresJar)) {
    Write-Error "Chua tim thay PostgreSQL JDBC driver tai $postgresJar. Can tai dependency org.postgresql:postgresql:42.7.5 truoc."
    exit 1
}

$classPath = "target/classes:$postgresJar"

java "-Dauction.server.host=$ServerHost" "-Dauction.server.port=$Port" "-Dauction.db.host=$DbHost" "-Dauction.db.port=$DbPort" "-Dauction.db.name=$DbName" "-Dauction.db.user=$DbUser" "-Dauction.db.password=$DbPassword" "-Dauction.db.sslmode=$DbSslMode" --module-path $modulePath --add-modules javafx.controls,javafx.fxml -cp $classPath ClientMain