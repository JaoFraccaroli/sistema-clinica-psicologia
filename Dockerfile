# Imagem da clínica em duas etapas:
# 1) compila com Maven (o Maven fica só dentro do container: não precisa instalar no PC)
# 2) roda apenas o .jar numa imagem enxuta com Java 17

FROM maven:3.9-eclipse-temurin-17 AS compilacao
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Os testes rodam no GitHub Actions; aqui só empacota
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
# Aplica as correções de segurança do sistema que saíram depois da imagem base
RUN apt-get update && apt-get upgrade -y --no-install-recommends && rm -rf /var/lib/apt/lists/*
# Não roda como root: se alguém invadir a aplicação, não ganha o container inteiro
RUN useradd --system --uid 1001 clinica && mkdir -p /app/dados && chown clinica /app/dados
COPY --from=compilacao /app/target/*.jar app.jar
USER clinica
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
