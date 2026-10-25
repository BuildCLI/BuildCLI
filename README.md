# 🌦️ Weather API

<p align="center">
  <strong>Uma API REST em Go para consulta de informações meteorológicas por cidade.</strong>
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
  <a href="#endpoints">Endpoints</a> •
  <a href="#instalação-e-execução">Instalação</a> •
  <a href="#utilização">Utilização</a> •
  <a href="#contribuição">Contribuição</a>
</p>

---

# Sumário

- #visão-geral
- #funcionalidades
- #tecnologias
- #arquitetura
- #endpoints
- #pré-requisitos
- #instalação-e-execução
- #utilização
- #exemplo-de-resposta
- #tratamento-de-erros
- #clientes-web
- #contribuição
- #licença

---

# Visão Geral

O Weather API é um projeto desenvolvido em Go que disponibiliza endpoints HTTP para consulta de informações meteorológicas por cidade.

A API fornece informações como:

- 🌡️ Temperatura atual
- 💨 Velocidade do vento
- 🌤️ Descrição das condições climáticas
- 📅 Previsão do tempo

O projeto pode ser utilizado para:

- Estudos de APIs REST
- Integração entre aplicações
- Desenvolvimento em Go
- Consumo de dados meteorológicos

## Repositório

https://github.com/robertoduessmann/weather-api

> Observação: a disponibilidade do serviço público e o contrato exato das respostas dependem da implementação atual do projeto.

---

# Funcionalidades

- 🌡️ Consulta de temperatura por cidade
- 💨 Consulta de velocidade do vento
- 🌤️ Consulta das condições climáticas atuais
- 📅 Consulta de previsão meteorológica
- 🔌 Integração via HTTP REST
- 📦 Resposta em formato JSON
- 🖥️ Compatível com aplicações web, mobile e desktop

---

# Tecnologias

- Go
- HTTP REST
- JSON
- Git
- curl

---

# Arquitetura

```text
Cliente HTTP
     │
     ▼
 Weather API
     │
     ▼
 Serviço Meteorológico
     │
     ▼
 Resposta JSON
```

Fluxo básico:

1. O cliente envia uma requisição contendo uma cidade.
2. A API consulta o serviço meteorológico configurado.
3. Os dados são processados.
4. A resposta é retornada em JSON.

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

### Exemplo com curl

```bash
curl "https://goweather.xyz/v2/weather/Curitiba"
```

Substitua `{city}` pelo nome da cidade desejada.

> A disponibilidade do endpoint público deve ser validada antes do uso em produção.

---

## API Local

Com a aplicação em execução:

```http
GET http://localhost:3000/weather/{city}
```

### Exemplo

```bash
curl "http://localhost:3000/weather/Curitiba"
```

---

# Pré-requisitos

Instale os seguintes softwares:

- Go
- Git
- curl

Verifique as instalações:

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

## 2. Compilar o projeto

```bash
go build
```

---

## 3. Executar a aplicação

### Linux e macOS

```bash
./weather-api
```

### Windows

```powershell
.\weather-api.exe
```

---

## 4. Testar a API

```bash
curl "http://localhost:3000/weather/Curitiba"
```

---

# Utilização

## Consultar uma cidade

### Requisição

```http
GET /weather/Curitiba
```

### Usando curl

```bash
curl -i "http://localhost:3000/weather/Curitiba"
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

	if err := json.NewDecoder(resp.Body).Decode(&weather); err != nil {
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

> Os valores acima são fictícios e servem apenas para ilustrar o formato da resposta.

---

## Estrutura da Resposta

| Campo | Tipo | Descrição |
|---------|---------|---------|
| `temperature` | string | Temperatura atual |
| `wind` | string | Velocidade do vento |
| `description` | string | Descrição das condições climáticas |
| `forecast` | array | Lista de previsões |
| `forecast[].day` | string | Dia da previsão |
| `forecast[].temperature` | string | Temperatura prevista |
| `forecast[].wind` | string | Velocidade prevista do vento |

---

# Tratamento de Erros

A API pode retornar códigos HTTP indicando o resultado da operação.

| Status | Descrição |
|----------|----------|
| `200 OK` | Consulta realizada com sucesso |
| `404 Not Found` | Cidade não encontrada |
| `500 Internal Server Error` | Erro interno do servidor |

## Exemplo de retorno de erro

```json
{
  "message": "city not found"
}
```

> Os códigos acima devem ser confirmados conforme a implementação atual da API.

---

# Clientes Web

Os seguintes projetos utilizam ou se inspiram nesta API:

## React Tempo

- Aplicação: https://reacttempo.netlify.app/
- Repositório: https://github.com/GabrielCampos99/appTempo

## Weather App

- Aplicação: https://emaniaditya.github.io/weather-app
- Repositório: https://github.com/emaniaditya/weather-app

## Weather React TSX

- Aplicação: https://weather-react-tsx.netlify.app/
- Repositório: https://github.com/keissiant/weather-api-react.tsx

> A compatibilidade desses clientes com a versão atual da API deve ser verificada individualmente.

---

# Contribuição

Contribuições são bem-vindas.

## Como contribuir

1. Faça um fork do projeto.
2. Clone seu fork.
3. Crie uma branch para sua alteração.
4. Implemente a melhoria ou correção.
5. Execute os testes.
6. Atualize a documentação quando necessário.
7. Abra um Pull Request.

### Criar uma branch

```bash
git checkout -b feature/minha-melhoria
```

### Registrar alterações

```bash
git add .
git commit -m "Improve weather API documentation"
```

### Enviar alterações

```bash
git push origin feature/minha-melhoria
```

---

## Boas práticas

- Mantenha alterações focadas.
- Não inclua credenciais ou informações sensíveis.
- Atualize a documentação quando necessário.
- Adicione testes para novas funcionalidades.
- Siga os padrões existentes do projeto.

---

# Licença

Este projeto é distribuído sob a licença MIT.

Consulte o arquivo:

```text
LICENSE
```

para obter os termos completos.

---

# Links Úteis

- Código-fonte: https://github.com/robertoduessmann/weather-api
- Issues: https://github.com/robertoduessmann/weather-api/issues
- Releases: https://github.com/robertoduessmann/weather-api/releases
- Go: https://go.dev/
- Documentação do Go: https://go.dev/doc/

---

<p align="center">
  Desenvolvido com Go para facilitar o acesso a informações meteorológicas.
</p>
