FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl default-mysql-client \
	&& rm -rf /var/lib/apt/lists/* \
	&& groupadd --system gestourant \
	&& useradd --system --gid gestourant --home-dir /app --shell /usr/sbin/nologin gestourant \
	&& mkdir -p /app/logs \
	&& chown -R gestourant:gestourant /app

WORKDIR /app
COPY --from=build --chown=gestourant:gestourant /workspace/target/gestourant-backend-0.0.1-SNAPSHOT.jar /app/gestourant.jar

USER gestourant
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/gestourant.jar"]
