FROM docker.io/gradle:8.3.0 as Builder
COPY . /app/
ARG CI_COMMIT_TAG
ENV CI_COMMIT_TAG $CI_COMMIT_TAG
WORKDIR /app
RUN gradle app:build

FROM docker.io/eclipse-temurin:17
COPY --from=Builder /app/app/build/libs/genre-police.jar genre-police.jar
ENTRYPOINT ["java", "-jar", "/genre-police.jar"]