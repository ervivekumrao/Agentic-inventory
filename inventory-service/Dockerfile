FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r appgroup && useradd -r -u 10001 -g appgroup appuser
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN chown appuser:appgroup /app.jar
USER 10001
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java","-jar","/app.jar"]
