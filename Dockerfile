# Runtime image for the booking application (specs/04_deployment/01-deployment.spec.adoc).
# Build the JAR first: ./gradlew -Pvaadin.productionMode=true bootJar
FROM eclipse-temurin:21-jre

# Unprivileged user; the app writes only to the file storage and the Jmix work directory
RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/filestorage /app/.jmix \
    && chown -R app:app /app

WORKDIR /app
COPY --chown=app:app build/libs/booking.jar /app/booking.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/booking.jar"]
