# 🌤️ Weather API

> **API REST simples e resiliente desenvolvida em Java 17 e Spring Boot 3 para consulta de condições meteorológicas em tempo real através da integração com o provedor Open-Meteo.**

![Java 17](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3+-brightgreen?style=for-the-badge&logo=springboot)
![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Build Status](https://img.shields.io/badge/Tests-Passing-success?style=for-the-badge&logo=githubactions)

---

## 📌 Sobre o Projeto

A **Weather API** é uma solução leve e moderna projetada para consultar dados meteorológicos atuais de qualquer cidade sem a necessidade de chaves de API (*API Keys*). O projeto adota princípios de **Arquitetura Limpa** (Hexagonal/Ports & Adapters) e **Testes FIRST**, garantindo isolamento de rede nos testes unitários e alta resiliência na comunicação com provedores externos via timeouts e tratamento refinado de exceções HTTP.

### ✨ Principais Funcionalidades
* **Consulta Meteorológica:** Temperatura, sensação térmica, umidade, velocidade do vento e tradução dos códigos WMO em português.
* **Resiliência e Mapeamento HTTP:** Tratamento automático para *Timeouts* (504), *Cidade Não Encontrada* (404), *Validações de Entrada* (400) e *Falhas do Provedor* (502).
* **Porta Configurável:** Suporte nativo à injeção da variável de ambiente `PORT` para fácil implantação em provedores Cloud (Render, Heroku, AWS).
* **Health Check Integrado:** Endpoint `/health` para monitoramento contínuo da aplicação.

---

## 🏛️ Arquitetura e Boas Práticas

O projeto foi desenvolvido seguindo os princípios da **Arquitetura Hexagonal (Ports & Adapters)** e **Clean Code**, garantindo um baixo acoplamento entre as regras de negócio da aplicação e as integrações externas (como a comunicação HTTP com a API do Open-Meteo).

```text
               +----------------------------------+
               |         Camada de Entrada        |
               | (WeatherController / REST API)   |
               +----------------+-----------------+
                                |
                                v
               +----------------------------------+
               |        Regras de Negócio         |
               |        (WeatherService)          |
               +----------------+-----------------+
                                |
                                v  [Port]
               +----------------------------------+
               |         WeatherProvider          |
               +----------------+-----------------+
                                |
                                v  [Adapter]
               +----------------------------------+
               |        OpenMeteoProvider         |
               |      (RestClient / Open-Meteo)   |
               +----------------+-----------------+
