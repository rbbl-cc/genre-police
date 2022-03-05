FROM openjdk:11

COPY build/libs/genre-police.jar genre-police.jar
ENTRYPOINT ["java","-jar","/genre-police.jar"]