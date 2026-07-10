# ---- Build stage: compila el binario nativo (glibc, dinámico) ----
FROM ghcr.io/graalvm/native-image-community:25 AS build

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN ./gradlew --no-daemon --version

COPY checkstyle/ checkstyle/
COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon nativeCompile

# ---- Runtime stage: solo el binario nativo, sin JVM ----
FROM gcr.io/distroless/base-debian12:nonroot

WORKDIR /app

COPY --from=build /workspace/build/native/nativeCompile/application ./application

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["/app/application"]
