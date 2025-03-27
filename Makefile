JDBC_JAR=sqlite-jdbc-3.49.1.0.jar
DOTENV_JAR=dotenv-java-3.2.0.jar
MAIN_CLASS=TerribleGame
CLASSPATH=.:$(JDBC_JAR):$(DOTENV_JAR)

.PHONY: all run clean download-jdbc download-dotenv

all: $(JDBC_JAR) $(DOTENV_JAR)
	javac -cp $(CLASSPATH) $(MAIN_CLASS).java

run: all
	java -cp $(CLASSPATH) $(MAIN_CLASS)

download-jdbc:
	curl -L -o $(JDBC_JAR) https://github.com/xerial/sqlite-jdbc/releases/download/3.49.1.0/$(JDBC_JAR)

download-dotenv:
	curl -L -o $(DOTENV_JAR) "https://repo1.maven.org/maven2/io/github/cdimascio/dotenv-java/3.2.0/$(DOTENV_JAR)"

$(JDBC_JAR): download-jdbc

$(DOTENV_JAR): download-dotenv

clean:
	rm -f *.class *.jar
