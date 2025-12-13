* * *

🧠 Pokédex API - Java
=====================

Uma **REST API** desenvolvida com **Spring Boot 3.5.0** e **Java 17**, que consome dados da PokéAPI e armazena localmente em **MySQL**. Permite buscar Pokémon por diversos critérios e possui um **modo de busca avançado** com filtros combinados.

O frontend Angular já foi buildado e colocado em `static` dentro do backend, permitindo acesso completo via navegador. A aplicação está hospedada em uma **máquina virtual no DigitalOcean**, com IP público configurado para acesso.

* * *

📑 Features
-----------

- 🔎 **Buscas por Pokémon por:**
  
  - ID
  
  - Nome
  
  - Tipo
  
  - Habilidade
  
  - Movimento
  
  - Região

- 🧬 **Busca avançada:**
  
  - Combinação de filtros por múltiplos tipos, habilidades, movimentos e regiões.

- 🧠 **Automação:**
  
  - Preenchimento automático da base de dados ao iniciar o projeto.

- 🐳 **Docker:**
  
  - Containers configurados para backend e MySQL, incluindo frontend buildado.

- 🔄 **Banco de dados:**
  
  - Criação automática de tabelas via Hibernate.

* * *

🛠️ Tecnologias Utilizadas
--------------------------

* **Java 17**

* **Spring Boot 3.5.0**

* **Hibernate / JPA**

* **Maven**

* **MySQL**

* **Docker / Docker Compose**

* **Angular (frontend buildado em `dist` dentro de `static`)**

* **DigitalOcean (VM e hospedagem)**

* **IntelliJ IDEA 2025.1**

* * *

👨‍💻 Como Rodar Localmente ou na VPS
-------------------------------------



### ✅ Pré-requisitos

* Docker e Docker Compose instalados

* Java 17

* Maven (ou `./mvnw`)



### 🚀 Passo a passo

1. Clone o repositório:
   
   ```
   git clone https://github.com/marcoscunhaa/Pokedex-with-springboot.git`
   ```

2. Acesse a pasta do projeto:
   
   ```
   cd Pokedex-with-springboot
   ```

3. Rode o build e baixe dependências:
   
   ```
   mvn clean install
   ```

4. Suba os containers com Docker Compose:
   
   ```
   docker-compose up -d
   ```

5. Acesse a aplicação pelo navegador:
   
   ```
   http://http://137.184.186.231:8080/
   ```


