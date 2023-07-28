FROM docker.io/gradle:8.2.1 as Builder
COPY . /app/
WORKDIR /app/app
RUN gradle build

FROM docker.io/eclipse-temurin:17
COPY --from=Builder /app/app/build/libs/genre-police.jar genre-police.jar
ENTRYPOINT ["java", "-jar", "/genre-police.jar"]