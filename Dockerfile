FROM gradle:9.8.1-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle :app:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/app/install/app .
EXPOSE 8080
ENTRYPOINT ["bin/app"]
