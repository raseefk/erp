FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S levanto && adduser -S levanto -G levanto

COPY --from=build /app/target/flooring-1.0.0.jar app.jar

RUN mkdir -p /app/data && chown -R levanto:levanto /app
USER levanto

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
