# =========================
# 1) Build stage (Gradle로 JAR 생성)
# =========================
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Gradle Wrapper 먼저 복사 (캐시 효율)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew --no-daemon clean bootJar

# =========================
# 2) Run stage (JRE로 실행)
# =========================
FROM eclipse-temurin:17-jre
WORKDIR /app

# build/libs 아래 생성된 jar를 app.jar로 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Render는 PORT(기본 10000)를 사용. Spring은 application.yml의 ${PORT}로 바인딩.
EXPOSE 10000

CMD ["sh","-c","java -jar app.jar"]
