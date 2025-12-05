

# 🧠 Pokédex API - Java

A REST API developed with **Spring Boot 3.5.0** and **Java 17**, which consumes data from the [PokéAPI](https://pokeapi.co/api/v2/) and stores it locally in a **MySQL** database. The system allows you to search for Pokémon by multiple criteria, in addition to including an advanced search mode with combined filters.

* * *

## 📑 Features

* 🔎 Search for Pokémon by:
  * ID
  * Name
  * Type
  * Ability
  * Move
  * Region
* 🧬 Advanced search:
  * Combined filtering by multiple types, abilities, moves and regions.
* 🧠 Automatic database filling when starting the project.
* 🐳 Docker container configured with Dockerfile.
* 🌐 **dev** and **prod** environment mode with `spring.profiles.active`.
* ☁️ Hosting via **Railway** with MySQL database provisioned in the cloud.
* 🔄 Automatic generation of tables by **Hibernate**.

* * *

## 🛠️ Technologies Used

* Java 17

* Spring Boot 3.5.0

* Hibernate / JPA

* Maven

* MySQL

* Docker / Docker Compose

* Railway (deploy and database)

* IntelliJ IDEA 2025.1

---

👨‍💻 How to Run in Development (Dev)
--------------------------------------------

Follow the steps below to run the project locally with a development profile:

### ✅ Prerequisites

* Docker and Docker Compose installed

* Java 17 installed

* Maven (or `./mvnw` if you prefer to use the wrapper)

### 🚀 Step by Step

**1. Clone the repository**

```
git clone https://github.com/marcoscunhaa/Pokedex-with-springboot.git
```

**2. Walk to the project**

```
cd Pokedex-with-springboot
```

**3. Go to src/main/resources/application.properties and change the line**

```
spring.profiles.active=dev
```

**4. Run `mvn clean install` to download dependencies and build the project.**

**5. Import the project into your favorite IDE.**

**6. Start the Spring Boot application.**

---

🎯 Application running
-------------------

![pokedex-app](assets/pokedex-application.gif "pokedex-app")
