#!/bin/sh
# Find the main project file
SOLUTION_FILE=$(find . -name "*.sln" | head -1)
echo "Using project file: $SOLUTION_FILE"
dotnet clean "$SOLUTION_FILE" && dotnet test "$SOLUTION_FILE" --logger "trx;LogFileName=testResults.trx"


