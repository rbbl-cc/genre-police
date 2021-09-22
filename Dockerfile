FROM openjdk:11

COPY build/libs/genre-police-*-all.jar genre-police.jar
ENTRYPOINT ["java","-jar","/genre-police.jar"]