#!/usr/bin/env bash
set -euo pipefail

SERVER_HOST="localhost"
PORT=5050
DB_HOST=""
DB_PORT=5432
DB_NAME="postgres"
DB_USER=""
DB_PASSWORD=""
DB_SSLMODE=""
JAVA_FX_VERSION="21.0.6"
POSTGRES_VERSION="42.7.5"
PLATFORM="mac"
ARCH="$(uname -m)"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --server-host)
      SERVER_HOST="${2:?Missing value for --server-host}"
      shift 2
      ;;
    --port)
      PORT="${2:?Missing value for --port}"
      shift 2
      ;;
    --db-host)
      DB_HOST="${2:?Missing value for --db-host}"
      shift 2
      ;;
    --db-port)
      DB_PORT="${2:?Missing value for --db-port}"
      shift 2
      ;;
    --db-name)
      DB_NAME="${2:?Missing value for --db-name}"
      shift 2
      ;;
    --db-user)
      DB_USER="${2:?Missing value for --db-user}"
      shift 2
      ;;
    --db-password)
      DB_PASSWORD="${2:?Missing value for --db-password}"
      shift 2
      ;;
    --db-sslmode)
      DB_SSLMODE="${2:?Missing value for --db-sslmode}"
      shift 2
      ;;
    -h|--help)
      cat <<'EOF'
Usage: ./run-app-macos.sh [options]
  --server-host <host>
  --port <port>
  --db-host <host>
  --db-port <port>
  --db-name <name>
  --db-user <user>
  --db-password <password>
  --db-sslmode <mode>
EOF
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

find_javafx_jar() {
  local artifact="$1"
  local version_dir="$HOME/.m2/repository/org/openjfx/$artifact/21.0.6"
  local generic="$version_dir/$artifact-21.0.6.jar"
  local preferred_names=()

  if [[ "$ARCH" == "aarch64" || "$ARCH" == "arm64" ]]; then
    preferred_names+=("$artifact-21.0.6-$PLATFORM-aarch64.jar")
  fi
  preferred_names+=("$artifact-21.0.6-$PLATFORM.jar")

  for jar_name in "${preferred_names[@]}"; do
    if [[ -f "$version_dir/$jar_name" ]]; then
      printf '%s\n' "$version_dir/$jar_name"
      return 0
    fi
  done

  if [[ -f "$generic" ]]; then
    printf '%s\n' "$generic"
    return 0
  fi

  return 1
}

JAVA_FX_JARS=()
for artifact in javafx-base javafx-controls javafx-graphics javafx-fxml; do
  if ! jar_path="$(find_javafx_jar "$artifact")"; then
    echo "Khong tim thay $artifact 21.0.6 cho macOS trong ~/.m2. Hay tai dependency JavaFX tren may macOS truoc." >&2
    exit 1
  fi
  JAVA_FX_JARS+=("$jar_path")
done

POSTGRES_JAR="$HOME/.m2/repository/org/postgresql/postgresql/$POSTGRES_VERSION/postgresql-$POSTGRES_VERSION.jar"
if [[ ! -f "$POSTGRES_JAR" ]]; then
  echo "Khong tim thay PostgreSQL JDBC driver tai $POSTGRES_JAR. Hay tai org.postgresql:postgresql:$POSTGRES_VERSION truoc." >&2
  exit 1
fi

# Tách biệt thư mục output của Client để tránh ghi đè chéo với Server
TARGET_DIR="target/client-classes"
mkdir -p "$TARGET_DIR"

# Tạo danh sách file tạm riêng biệt cho client, kiểm tra thư mục tồn tại trước khi find
SOURCES_FILE="sources-client.txt"
> "$SOURCES_FILE"
if [[ -d "client/java" ]]; then
  find client/java -name '*.java' -print >> "$SOURCES_FILE"
fi
if [[ -d "src/main/java" ]]; then
  find src/main/java -name '*.java' -print >> "$SOURCES_FILE"
fi

# Kiểm tra nếu không tìm thấy file mã nguồn nào
if [[ ! -s "$SOURCES_FILE" ]]; then
  echo "Loi: Khong tim thay bat ky file .java nao de bien dich!" >&2
  exit 1
fi

MODULE_PATH=$(IFS=:; echo "${JAVA_FX_JARS[*]}")
javac --module-path "$MODULE_PATH" --add-modules javafx.controls,javafx.fxml -d "$TARGET_DIR" @"$SOURCES_FILE"

# Kiểm tra an toàn trước khi copy tài nguyên giao diện của Client
if [[ -d "client/resources" ]]; then
  cp -R client/resources/. "$TARGET_DIR"/
fi

CLASS_PATH="$TARGET_DIR:$POSTGRES_JAR"
JAVA_ARGS=(
  "-Dauction.server.host=$SERVER_HOST"
  "-Dauction.server.port=$PORT"
)

if [[ -n "${DB_HOST// }" ]]; then
  JAVA_ARGS+=("-Dauction.db.host=$DB_HOST")
fi
# Đồng bộ hóa logic nhận diện DB_PORT độc lập tương tự như bên Server
if [[ -n "${DB_PORT// }" ]]; then
  JAVA_ARGS+=("-Dauction.db.port=$DB_PORT")
fi
if [[ -n "${DB_NAME// }" && -n "${DB_HOST// }" ]]; then
  JAVA_ARGS+=("-Dauction.db.name=$DB_NAME")
fi
if [[ -n "${DB_USER// }" ]]; then
  JAVA_ARGS+=("-Dauction.db.user=$DB_USER")
fi
if [[ -n "${DB_PASSWORD// }" ]]; then
  JAVA_ARGS+=("-Dauction.db.password=$DB_PASSWORD")
fi
if [[ -n "${DB_SSLMODE// }" ]]; then
  JAVA_ARGS+=("-Dauction.db.sslmode=$DB_SSLMODE")
fi

# Dọn dẹp file tạm trước khi khởi chạy ứng dụng
rm -f "$SOURCES_FILE"

exec java "${JAVA_ARGS[@]}" \
  --enable-native-access=javafx.graphics \
  --module-path "$MODULE_PATH" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "$CLASS_PATH" \
  ClientMain
