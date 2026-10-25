🌦️ Weather API Java
API REST meteorológica desenvolvida em Java com Spring Boot. Consulta dados do serviço wttr.in e retorna temperatura, vento, descrição das condições climáticas e previsão em JSON.
Tecnologias
Java 17
Spring Boot
Maven
REST/JSON
JUnit e Spring MockMvc
wttr.in
Pré-requisitos
JDK 17 ou superior
Maven 3.8+ (ou Maven Wrapper, caso adicionado ao projeto)
Verifique:
java -version
mvn -version
Compilar e gerar o JAR
Na raiz do projeto:
mvn clean test package
O artefato executável será gerado em:
target/weather-api-java.jar
Executar
java -jar target/weather-api-java.jar
A aplicação usa a porta 3000 por padrão. Para alterar a porta:
Linux/macOS:
PORT=8080 java -jar target/weather-api-java.jar
PowerShell:
$env:PORT = "8080"
java -jar target/weather-api-java.jar
Endpoint
GET /weather/{city}
Exemplo:
curl "http://localhost:3000/weather/Curitiba"
Resposta ilustrativa:
{
  "city": "Curitiba",
  "temperature": "24 °C",
  "wind": "10 km/h",
  "description": "Partly cloudy",
  "forecast": [
    {
      "day": "1",
      "temperature": "25 °C",
      "wind": "12 km/h"
    }
  ]
}
Os valores acima são exemplos; os dados reais dependem do serviço externo.
Erros
404 Not Found: dados meteorológicos não encontrados.
502 Bad Gateway: serviço meteorológico externo indisponível.
Testes
mvn test
Observações
O cache em memória tem validade de dois minutos.
A aplicação depende da disponibilidade e do formato de resposta do wttr.in.
Este projeto Java é uma implementação separada; não é um JAR produzido a partir do código Go original.
Licença
Adicione ou confirme o arquivo LICENSE conforme a licença escolhida para o projeto.