# 多阶段构建：本机无需安装 JDK/Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 先拷 pom 单独下载依赖，利用 Docker 层缓存加速后续构建
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
