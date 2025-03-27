JDBC_JAR=sqlite-jdbc-3.49.1.0.jar
DOTENV_JAR=dotenv-java-3.2.0.jar
BCRYPT_JAR=jbcrypt-0.4.jar
MAIN_CLASS=TerribleGame
CLASSPATH=.:$(JDBC_JAR):$(DOTENV_JAR):$(BCRYPT_JAR)

.PHONY: all run clean download-jdbc download-dotenv download-bcrypt

all: $(JDBC_JAR) $(DOTENV_JAR) $(BCRYPT_JAR)
	javac -cp $(CLASSPATH) $(MAIN_CLASS).java

run: all
	java -cp $(CLASSPATH) $(MAIN_CLASS)

download-jdbc:
	curl -L -o $(JDBC_JAR) https://github.com/xerial/sqlite-jdbc/releases/download/3.49.1.0/$(JDBC_JAR)
download-dotenv:
	curl -L -o $(DOTENV_JAR) "https://repo1.maven.org/maven2/io/github/cdimascio/dotenv-java/3.2.0/$(DOTENV_JAR)"

download-bcrypt:
	curl -L -o $(BCRYPT_JAR) "https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/$(BCRYPT_JAR)"

$(JDBC_JAR): download-jdbc

$(DOTENV_JAR): download-dotenv

$(BCRYPT_JAR): download-bcrypt

clean:
	rm -f *.class *.jar
	rm -f *.class *.jar
