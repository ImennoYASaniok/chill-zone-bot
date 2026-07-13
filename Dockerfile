FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /workspace
COPY . .
RUN gradle :app:installDist --no-daemon

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=build /workspace/app/build/install/app /app
CMD ["./bin/app"]
