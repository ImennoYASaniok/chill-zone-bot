FROM amazoncorretto:17-alpine
WORKDIR /app

# Install curl and download gradle
RUN apk add --no-cache curl unzip && \
    curl -L https://services.gradle.org/distributions/gradle-8.5-bin.zip -o gradle.zip && \
    unzip gradle.zip && \
    rm gradle.zip

ENV GRADLE_HOME=/app/gradle-8.5
ENV PATH=$PATH:/app/gradle-8.5/bin

COPY . .
RUN gradle :app:installDist --no-daemon

WORKDIR /app/app/build/install/app
CMD ["./bin/app"]
