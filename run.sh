#!/usr/bin/env bash
# ==============================================================================
# Online Fitness Coaching Platform - Build & Run Script
# ==============================================================================

set -e

# Detect or set Java
if [ -d "/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home" ]; then
    export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "================================================="
echo " Building Online Fitness Coaching Platform"
echo " Java version: $(java -version 2>&1 | head -n 1)"
echo "================================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

mkdir -p bin data

echo "[1/2] Compiling Java source files..."
find src -name "*.java" > sources.txt
javac -d bin @sources.txt
rm -f sources.txt

echo "[2/2] Launching platform on http://localhost:8080 ..."
java -cp bin com.fitness.Main
