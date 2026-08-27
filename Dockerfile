FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY . .
# Dosyaları Java paket yapısına uygun alt klasöre taşıyıp derliyoruz
RUN mkdir -p src/aimclassic && mv *.java src/aimclassic/ && javac -d bin src/aimclassic/*.java
EXPOSE 5190
CMD ["java", "-cp", "bin", "aimclassic.Hub"]
