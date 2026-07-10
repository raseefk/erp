FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S supererp && adduser -S supererp -G supererp

COPY --from=build /app/target/super-erp-single-tenant-1.0.0.jar app.jar

RUN mkdir -p /app/uploads /app/logs && chown -R supererp:supererp /app
USER supererp

EXPOSE 8087

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
