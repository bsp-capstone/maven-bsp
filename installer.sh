#!/usr/bin/env bash

if [[ "$#" -ne 1 ]]; then
	echo "Usage: ./installer.sh <project_path>"
    exit 1
fi

project_path=$1

if ! command -v mvn &> /dev/null
then
    echo "First, you need Maven installed."
    exit 1
fi

if ! command -v java &> /dev/null
then
    echo "First, you need Java installed."
    exit 1
fi

echo "Compiling and installing BSP server..."
mvn -q clean package
jar_name="server-1.0-SNAPSHOT-spring-boot.jar"
jar_path="$HOME/.bsp/maven/"
mkdir -p "$jar_path"
cp "server/target/$jar_name" "$jar_path"

echo "Creating connection file..."
mkdir -p "$project_path/.bsp" && cd "$project_path/.bsp"

connection="{
  \"name\": \"Maven BSP\",
  \"version\": \"1.0.0\",
  \"bspVersion\": \"1.0.0\",
  \"languages\": [\"Java\"],
  \"argv\": [\"java\", \"-jar\", \"$jar_path$jar_name\"]
}"

echo "$connection" > mavenbsp.json
echo "Successfully installed."
