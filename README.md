# 🌦️ Weather API

<p align="center">
  <strong>Uma API REST desenvolvida em Go para consulta de informações meteorológicas por cidade.</strong>
</p>

<p align="center">
  Consulte temperatura, velocidade do vento, descrição das condições climáticas e previsão do tempo por meio de endpoints HTTP simples e integráveis.
</p>

<p align="center">
  <a href="https://github.com/robertoduessmann/weather-api">
    <img src="https://img.shields.io/badge/Go-API-00ADD8?style=for-the-badge&logo=go&logoColor=white" alt="Go API" />
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-MIT-brightgreen.svg?style=for-the-badge" alt="MIT License" />
  </a>
</p>

<p align="center">
  <a href="https://goreportcard.com/report/github.com/robertoduessmann/weather-api">
    <img src="https://goreportcard.com/badge/github.com/robertoduessmann/weather-api" alt="Go Report Card" />
  </a>
  <a href="https://godoc.org/github.com/robertoduessmann/weather-api">
    <img src="https://godoc.org/github.com/robertoduessmann/weather-api?status.svg" alt="GoDoc" />
  </a>
</p>

<p align="center">
  <a href="#visão-geral">Visão Geral</a> •
  <a href="#funcionalidades">Funcionalidades</a> •
  <a href="#tecnologias">Tecnologias</a> •
  <a href="#arquitetura">Arquitetura</a> •
  <a href="#endpoints">Endpoints</a> •
  <a href="#instalação-e-execução">Instalação</a> •
  <a href="#testes">Testes</a> •
  <a href="#utilização">Utilização</a> •
  <a href="#contribuição">Contribuição</a>
</p>

---

# 📑 Sumário

- #visão-geral
- #funcionalidades
- #tecnologias
- #arquitetura
- #dependências
- #cache
- #endpoints
- #pré-requisitos
- #instalação-e-execução
- #testes
- #utilização
- #exemplo-de-resposta
- #tratamento-de-erros
- #clientes-web
- #contribuição
- #licença

---

# Visão Geral

O Weather API é uma API REST desenvolvida em Go para consulta de informações meteorológicas por cidade.

A aplicação permite consultar:

- 🌡️ Temperatura atual
- 💨 Velocidade do vento
- 🌤️ Condições climáticas
- 📅 Previsão do tempo

O projeto pode ser utilizado para:

- Estudos de APIs REST
- Integração entre aplicações
- Desenvolvimento em Go
- Aplicações web e mobile
- Consumo de dados meteorológicos

## Repositório Oficial

https://github.com/robertoduessmann/weather-api

---

# Funcionalidades

- Consulta de temperatura por cidade
- Consulta de velocidade do vento
- Consulta das condições climáticas atuais
- Consulta de previsão meteorológica
- API REST baseada em HTTP
- Respostas em JSON
- Cache de respostas
- Integração simples com aplicações externas

---

# Tecnologias

O projeto utiliza:

- Go
- HTTP REST
- JSON
- Gorilla Mux
- GoQuery
- Git
- curl
- wttr.in

---

# Arquitetura

A API utiliza o serviço **wttr.in** como fonte de dados meteorológicos.

```text
Cliente HTTP
     │
     ▼
Weather API
     │
     ▼
wttr.in
     │
     ▼
Parser HTML
     │
     ▼
Cache
     │
     ▼
Resposta JSON
```

Fluxo da aplicação:

1. O cliente envia uma cidade.
2. A API consulta o serviço wttr.in.
3. O HTML retornado é processado.
4. Os dados são convertidos para JSON.
5. O resultado é armazenado em cache.
6. A resposta é enviada ao cliente.

---

# Dependências

Principais dependências utilizadas:

```text
github.com/gorilla/mux
github.com/PuerkitoBio/goquery
```

Instalação das dependências:

```bash
go mod download
```

ou

```bash
go mod tidy
```

---

# Cache

A API implementa cache em memória para reduzir consultas repetidas ao serviço meteorológico.

Características:

- Cache por cidade consultada
- Armazenamento da resposta processada
- Expiração automática após 2 minutos

Benefícios:

- Menor latência
- Menor uso do serviço externo
- Melhor desempenho geral

---

# Endpoints

## API Pública

```http
GET https://goweather.xyz/v2/weather/{city}
```

### Exemplo

```http
GET https://goweather.xyz/v2/weather/Berlin
```

### Usando curl

```bash
curl "https://goweather.xyz/v2/weather/Curitiba"
```

Substitua `{city}` pelo nome da cidade desejada.

---

## API Local

Quando executada localmente:

```http
GET http://localhost:3000/weather/{city}
```

### Exemplo

```bash
curl "http://localhost:3000/weather/Curitiba"
```

---

# Pré-requisitos

Instale:

- Go
- Git
- curl

Verifique:

```bash
go version
git --version
curl --version
```

---

# Instalação e Execução

## 1. Clonar o repositório

```bash
git clone https://github.com/robertoduessmann/weather-api.git
```

```bash
cd weather-api
```

---

## 2. Instalar dependências

```bash
go mod download
```

ou

```bash
go mod tidy
```

---

## 3. Compilar

```bash
go build
```

---

## 4. Executar

### Linux / macOS

```bash
./weather-api
```

### Windows

```powershell
.\weather-api.exe
```

---

## 5. Testar

```bash
curl http://localhost:3000/weather/Curitiba
```

---

# Testes

O projeto possui testes automatizados para validar o comportamento dos componentes e endpoints da API.

## Executar todos os testes

```bash
go test ./...
```

## Saída detalhada

```bash
go test -v ./...
```

## Cobertura de testes

```bash
go test -cover ./...
```

## Relatório detalhado de cobertura

```bash
go test -coverprofile=coverage.out ./...
```

Visualizar relatório:

```bash
go tool cover -html=coverage.out
```

---

## Testes de Integração

Inicie a aplicação:

```bash
go build
./weather-api
```

Execute os testes:

```bash
go test -v ./...
```

### Cenário: Cidade válida

Requisição:

```http
GET /weather/London
```

Validações esperadas:

- Status HTTP 200
- JSON válido
- Campo `temperature`
- Campo `wind`
- Campo `description`
- Campo `forecast`

### Cenário: Cidade inexistente

Requisição:

```http
GET /weather/invalid-city-name-123456
```

Validações esperadas:

- Status HTTP 404
- JSON válido
- Campo `message`
- Valor `NOT_FOUND`

### Exemplo de teste de integração

```go
func TestWeatherEndpoint(t *testing.T) {

	resp, err := http.Get(
		"http://localhost:3000/weather/London",
	)

	if err != nil {
		t.Fatal(err)
	}

	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		t.Fatalf(
			"status inesperado: %d",
			resp.StatusCode,
		)
	}
}
```

---

## Verificação estática

```bash
go vet ./...
```

## Formatação

```bash
go fmt ./...
```

## Fluxo recomendado

```bash
go fmt ./...
go vet ./...
go test ./...
```

---

# Utilização

## Consulta simples

```http
GET /weather/Curitiba
```

### curl

```bash
curl -i http://localhost:3000/weather/Curitiba
```

---

## JavaScript (Fetch API)

```javascript
async function getWeather(city) {
  try {
    const response = await fetch(
      `https://goweather.xyz/v2/weather/${city}`
    );

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();

    console.log("Temperatura:", data.temperature);
    console.log("Vento:", data.wind);
    console.log("Descrição:", data.description);

  } catch (error) {
    console.error(error);
  }
}

getWeather("Curitiba");
```

---

## Go

```go
package main

import (
	"encoding/json"
	"fmt"
	"net/http"
)

type Weather struct {
	Temperature string `json:"temperature"`
	Wind        string `json:"wind"`
	Description string `json:"description"`
}

func main() {

	resp, err := http.Get(
		"https://goweather.xyz/v2/weather/Curitiba",
	)

	if err != nil {
		panic(err)
	}

	defer resp.Body.Close()

	var weather Weather

	if err := json.NewDecoder(resp.Body).
		Decode(&weather); err != nil {
		panic(err)
	}

	fmt.Println("Temperatura:", weather.Temperature)
	fmt.Println("Vento:", weather.Wind)
	fmt.Println("Descrição:", weather.Description)
}
```

---

# Exemplo de Resposta

## Content-Type

```http
Content-Type: application/json
```

## JSON

```json
{
  "temperature": "29 °C",
  "wind": "20 km/h",
  "description": "Partly cloudy",
  "forecast": [
    {
      "day": "1",
      "temperature": "27 °C",
      "wind": "12 km/h"
    },
    {
      "day": "2",
      "temperature": "22 °C",
      "wind": "8 km/h"
    }
  ]
}
```

## Estrutura da Resposta

| Campo | Tipo | Descrição |
|---------|---------|---------|
| `temperature` | string | Temperatura atual |
| `wind` | string | Velocidade do vento |
| `description` | string | Condições climáticas |
| `forecast` | array | Lista de previsões |
| `forecast[].day` | string | Dia previsto |
| `forecast[].temperature` | string | Temperatura prevista |
| `forecast[].wind` | string | Velocidade prevista do vento |

---

# Tratamento de Erros

Status identificados na implementação:

| Status | Descrição |
|---------|---------|
| `200 OK` | Consulta realizada com sucesso |
| `404 Not Found` | Cidade não encontrada ou falha na obtenção dos dados |

## Exemplo

```json
{
  "message": "NOT_FOUND"
}
```

---

# Clientes Web

## React Tempo

Aplicação:

https://reacttempo.netlify.app/

Repositório:

https://github.com/GabrielCampos99/appTempo

---

## Weather App

Aplicação:

https://emaniaditya.github.io/weather-app

Repositório:

https://github.com/emaniaditya/weather-app

---

## Weather React TSX

Aplicação:

https://weather-react-tsx.netlify.app/

Repositório:

https://github.com/keissiant/weather-api-react.tsx

---

# Contribuição

Contribuições são bem-vindas.

## Como contribuir

1. Faça um Fork.
2. Clone o Fork.
3. Crie uma branch.
4. Faça suas alterações.
5. Execute os testes.
6. Atualize a documentação.
7. Abra um Pull Request.

### Criar branch

```bash
git checkout -b feature/minha-melhoria
```

### Commit

```bash
git add .
git commit -m "Improve weather API documentation"
```

### Push

```bash
git push origin feature/minha-melhoria
```

## Boas práticas

- Manter alterações pequenas e objetivas.
- Atualizar a documentação.
- Adicionar testes quando possível.
- Não incluir credenciais.
- Seguir o padrão do projeto.

---

# Licença

Este projeto é distribuído sob a licença MIT.

Consulte o arquivo:

```text
LICENSE
```

para obter os termos completos.

---

# 🔗 Links Úteis

- Código-fonte: https://github.com/robertoduessmann/weather-api
- Issues: https://github.com/robertoduessmann/weather-api/issues
- Releases: https://github.com/robertoduessmann/weather-api/releases
- Go: https://go.dev
- Documentação Go: https://go.dev/doc

---

<p align="center">
  Desenvolvido com Go para facilitar o acesso a informações meteorológicas.
</p>
