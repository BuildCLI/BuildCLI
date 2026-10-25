Weather API — Java 17 + Maven
API REST simples para consultar as condições meteorológicas atuais de uma cidade usando o provedor público Open-Meteo. Não requer chave de API para este uso básico.
Requisitos
JDK 17 ou superior
Maven 3.9+
Acesso à internet para consultar o provedor e baixar dependências Maven
Executar testes e empacotar o JAR
mvn clean verify
O JAR executável, incluindo as dependências, será gerado em target/weather-api.jar.
java -jar target/weather-api.jar
A porta padrão é 8080. Para alterar:
Linux/macOS:
PORT=9090 java -jar target/weather-api.jar
Windows PowerShell:
$env:PORT=9090
java -jar target/weather-api.jar
Endpoints
Saúde
GET /health
Exemplo de resposta:
{"status":"UP","service":"weather-api"}
Condições atuais
GET /api/weather?city=Aracaju
Resposta inclui cidade, país, coordenadas, temperatura, sensação térmica, umidade, velocidade do vento, código/descrição e horário de observação.
Códigos HTTP
200: consulta concluída.
400: parâmetro ausente ou inválido.
404: cidade não encontrada pelo provedor.
405: método HTTP não permitido.
502: erro ou resposta inválida do provedor.
504: timeout do provedor.
500: erro interno inesperado.
Testes FIRST
Os testes unitários usam um WeatherProvider falso por injeção de dependência, sem rede externa. Eles cobrem cidade válida, entradas nulas/vazias, caracteres inválidos, limite de tamanho, falhas do provedor e descrição de códigos meteorológicos.
Fast: testes unitários sem chamadas de rede.
Independent: cada teste constrói seus próprios objetos.
Repeatable: resultados não dependem de um serviço externo.
Self-validating: asserções automáticas.
Timely: testes próximos da lógica implementada.
Integração do provedor
O cliente verifica o status HTTP antes de interpretar o corpo, aplica timeout, valida campos essenciais do JSON e distingue cidade não encontrada, timeout, indisponibilidade e resposta inválida. O serviço externo ainda pode mudar limites ou disponibilidade; erros são convertidos em respostas HTTP controladas.