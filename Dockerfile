FROM openjdk:11

COPY target/genre-police-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]