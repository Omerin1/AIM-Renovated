FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY . .
RUN javac -d bin *.java
EXPOSE 5190
CMD ["java", "-cp", "bin", "Hub"]
