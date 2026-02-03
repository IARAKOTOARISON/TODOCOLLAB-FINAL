#!/usr/bin/env bash
set -euo pipefail

# run.sh - build and run todolist-p2p
# Usage: ./run.sh

HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"

echo "[run.sh] Building project..."
mvn clean package -DskipTests

# Try to run via Maven exec (puts dependencies on the classpath)
echo "[run.sh] Attempting to run with 'mvn exec:java' (this is the recommended/run-in-dev method)..."
if mvn exec:java -Dexec.mainClass="com.todolistp2p.App" -Dexec.classpathScope=runtime; then
  echo "[run.sh] Application exited normally."
  exit 0
else
  echo "[run.sh] mvn exec:java failed or exited; will try fallback to java -jar if available."
fi

# Fallback: run assembled jar (if pom packaging produces it)
JAR="target/todolist-p2p-0.1.0-SNAPSHOT.jar"
if [ -f "$JAR" ]; then
  echo "[run.sh] Running jar: $JAR"
  if java -jar "$JAR"; then
    echo "[run.sh] Jar executed successfully."
    exit 0
  else
    echo "[run.sh] java -jar failed."
  fi
else
  echo "[run.sh] Fallback jar not found: $JAR"
fi

cat <<'EOF'
[run.sh] Done. If the application failed to start because of JavaFX runtime issues
you may need to provide JavaFX on the module-path. Example (adjust paths):

java --module-path /path/to/javafx/lib --add-modules javafx.controls -cp target/classes:target/dependency/* com.todolistp2p.App

Or install OpenJFX on your system and retry. If you want, I can add an assembly/fat-jar target in pom.xml to make distribution easier.
EOF
