

# 기존: FROM openjdk:17-jdk-slim
# 변경: 가장 안정적인 Amazon Corretto 또는 Eclipse Temurin 사용
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# JAR 파일 복사 (build/libs/ 아래에 빌드된 파일이 하나만 있어야 합니다)
COPY build/libs/*.jar app.jar

# 실행 권한 부여 (필요한 경우)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]