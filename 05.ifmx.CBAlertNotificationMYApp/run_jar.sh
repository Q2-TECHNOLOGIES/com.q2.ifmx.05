#!/bin/bash

# Path ke JAR file
JAR_PATH="/home/user/app/myapp.jar"

# Path ke file config
CONFIG_PATH="/home/user/app/config.properties"

# Jalankan aplikasi
java -jar "$JAR_PATH" --spring.config.location="$CONFIG_PATH"
