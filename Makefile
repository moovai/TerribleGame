# Maven-based build system for TerribleGame

# Variables
JAVA := /opt/homebrew/opt/openjdk@17/bin/java
MVN := mvn
JAR_NAME := target/terrible-game-1.0-SNAPSHOT-jar-with-dependencies.jar

.PHONY: all clean compile package run

# Default target
all: package

# Clean build artifacts
clean:
	$(MVN) clean

# Compile the source code
compile:
	$(MVN) compile

# Create executable jar with dependencies
package: compile
	$(MVN) package

# Run the application
run: package
	$(JAVA) -jar $(JAR_NAME)

# Run with debug options (optional)
debug: package
	$(JAVA) -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar $(JAR_NAME)

# Help target
help:
	@echo "Available targets:"
	@echo "  all     : Default target, builds the executable jar (same as package)"
	@echo "  clean   : Remove build artifacts"
	@echo "  compile : Compile the source code"
	@echo "  package : Create executable jar with dependencies"
	@echo "  run     : Run the application"
	@echo "  debug   : Run with remote debugging enabled on port 5005"
	@echo "  help    : Show this help message"
