#!/usr/bin/env bash
set -euo pipefail

PORT=5050
DB_HOST=""
DB_PORT=5432
DB_NAME="postgres"
DB_USER=""
DB_PASSWORD=""
DB_SSLMODE=""
JAVA_FX_VERSION="21.0.6"
POSTGRES_VERSION="42.7.5"
PLATFORM="linux"
ARCH="$(uname -m)"

while [[ $# -gt 0 ]]; do
  case "$1" in
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
Usage: ./run-server-linux.sh [options]
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
  local version_dir="$HOME/.m2/repository/org/openjfx/$artifact/$JAVA_FX_VERSION"
  local generic="$version_dir/$artifact-$JAVA_FX_VERSION.jar"
  local preferred_names=()

  if [[ "$ARCH" == "aarch64" || "$ARCH" == "arm64" ]]; then
    preferred_names+=("$artifact-$JAVA_FX_VERSION-$PLATFORM-aarch64.jar")
  fi
  preferred_names+=("$artifact-$JAVA_FX_VERSION-$PLATFORM.jar")

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
    echo "Khong tim thay $artifact $JAVA_FX_VERSION cho Linux trong ~/.m2. Hay tai dependency JavaFX tren may Linux truoc." >&2
    exit 1
  fi
  JAVA_FX_JARS+=("$jar_path")
done

POSTGRES_JAR="$HOME/.m2/repository/org/postgresql/postgresql/$POSTGRES_VERSION/postgresql-$POSTGRES_VERSION.jar"
if [[ ! -f "$POSTGRES_JAR" ]]; then
  echo "Khong tim thay PostgreSQL JDBC driver tai $POSTGRES_JAR. Hay tai org.postgresql:postgresql:$POSTGRES_VERSION truoc." >&2
  exit 1
fi

mkdir -p target/classes

find client/java -name '*.java' -print > sources.txt
find src/main/java -name '*.java' -print >> sources.txt

MODULE_PATH=$(IFS=:; echo "${JAVA_FX_JARS[*]}")
javac --module-path "$MODULE_PATH" --add-modules javafx.controls,javafx.fxml -d target/classes @sources.txt

CLASS_PATH="target/classes:$POSTGRES_JAR"
JAVA_ARGS=("-Dauction.server.port=$PORT")

if [[ -n "${DB_HOST// }" ]]; then
  JAVA_ARGS+=("-Dauction.db.host=$DB_HOST")
fi
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

exec java "${JAVA_ARGS[@]}" \
  --module-path "$MODULE_PATH" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "$CLASS_PATH" \
  server.ServerLauncherMain
