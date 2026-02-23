FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
# Install PostgreSQL client tools (pg_isready + psql)
RUN apt-get update && apt-get install -y findutils wget postgresql-client && \
    rm -rf /var/lib/apt/lists/*
RUN mkdir libs && cd libs && \
    # --- PostgreSQL JDBC driver ---
    wget -q https://jdbc.postgresql.org/download/postgresql-42.7.3.jar
COPY src ./src
COPY wait-for-postgres.sh .
RUN find src -name "*.java" > sources.txt && \
    mkdir -p out && \
    javac -cp "libs/*" -d out @sources.txt
RUN jar cvfe app.jar com.notes.Main -C out .
EXPOSE 8080
ENTRYPOINT ["./wait-for-postgres.sh", "db", "java", "-cp", "app.jar:libs/*", "com.notes.Main"]