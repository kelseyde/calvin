EXE = calvin-chess-engine
ifndef MAVEN_EXE
	MAVEN_EXE = mvn
endif

all:
ifdef JAVA_HOME
	JAVA_HOME=$(JAVA_HOME) $(MAVEN_EXE) -f ./pom.xml package
else
	$(MAVEN_EXE) -f ./pom.xml package
endif
	cat header.sh ./target/calvin-chess-engine-5.1.0-SNAPSHOT.jar > $(EXE)
	chmod +x $(EXE)
	cp ./target/calvin-chess-engine-5.1.0-SNAPSHOT.jar calvin-chess-engine.jar