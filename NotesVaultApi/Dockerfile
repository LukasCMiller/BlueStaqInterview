FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
RUN mkdir libs && cd libs && \
    # --- PostgreSQL JDBC driver ---
    wget -q https://jdbc.postgresql.org/download/postgresql-42.7.3.jar
COPY src ./src
RUN find src -name "*.java" > sources.txt && \
    mkdir -p out && \
    javac -cp "libs/*" -d out @sources.txt
RUN jar cvfe app.jar com.notes.Main -C out .
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "app.jar:libs/*", "com.notes.Main"]