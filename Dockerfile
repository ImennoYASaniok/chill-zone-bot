FROM gradle:8.8-jdk17 AS build
WORKDIR /home/gradle/project

COPY --chown=gradle:gradle . .
RUN gradle :app:installDist --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /home/gradle/project/app/build/install/app/ /app/

CMD ["/app/bin/app"]
