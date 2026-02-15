#!/bin/bash
echo "Building Chronicle Project..."
mvn clean package
echo ""
echo "Launching Chronicle..."
java -jar target/chronicle-rpg-1.0-SNAPSHOT-jar-with-dependencies.jar