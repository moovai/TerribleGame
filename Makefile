JDBC_JAR=sqlite-jdbc-3.49.1.0.jar
MAIN_CLASS=TerribleGame
CLASS_FILES=$(MAIN_CLASS).class $(MAIN_CLASS)$$GamePanel.class $(MAIN_CLASS)$$1.class

.PHONY: all run clean download-jdbc

all: $(CLASS_FILES)

%.class: %.java $(JDBC_JAR)
	javac -cp .:$(JDBC_JAR) $<

run: $(CLASS_FILES)
	java -cp .:$(JDBC_JAR) $(MAIN_CLASS)

download-jdbc:
	curl -L -o $(JDBC_JAR) https://github.com/xerial/sqlite-jdbc/releases/download/3.49.1.0/$(JDBC_JAR)

$(JDBC_JAR): download-jdbc

clean:
	rm -f *.class
