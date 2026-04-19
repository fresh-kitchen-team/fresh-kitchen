# GitHub Actions에서 이미 빌드했으니까
# jar 파일만 복사해서 실행하면 됨
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]