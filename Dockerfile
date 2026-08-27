FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN javac -d bin *.java || javac -d bin src/aimclassic/*.java src/aimclassic/Models/*.java || javac -d bin aimclassic/*.java
EXPOSE 5190
CMD ["java", "-cp", "bin", "aimclassic.Hub"]
