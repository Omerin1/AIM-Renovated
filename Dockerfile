FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY . .
# Dosyaları derleyip doğrudan paket yapısına uygun klasöre koyuyoruz
RUN mkdir -p bin/aimclassic && javac -d bin *.java && cp bin/*.class bin/aimclassic/ 2>/dev/null || true
EXPOSE 5190
# Hem düz hem de paketli olarak çalıştırmayı deniyoruz
CMD ["java", "-cp", "bin", "aimclassic.Hub"]
