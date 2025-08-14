# Library Management System

The Library Management System is a Java-based backend application that follows the Hexagonal Architecture pattern. It facilitates the management of books, authors, and transactions for a library. The system provides functionalities to manage book catalogs, register customers, process borrowing and returning transactions, and view borrowing history.

## Technologies and Dependencies Used

- **[Java](https://www.oracle.com/java/)**: Core programming language.
- **[Spring Boot](https://spring.io/projects/spring-boot)**: Used to build hassle-free web applications and write REST APIs.
- **[Spring Data JPA (Hibernate)](https://spring.io/projects/spring-data-jpa)**: Reduces the time of writing hardcoded SQL queries, allowing for more readable and scalable code.
- **[Spring Security](https://spring.io/projects/spring-security)**: Used for authentication and authorization.
- **[H2](https://www.h2database.com/html/main.html)**: Used as a persistence store for development and testing.
- **[Apache Maven](https://maven.apache.org/)**: Dependency management and build tool.
- **[Docker](https://www.docker.com/)**: Containerization for deployment.
- **[JUnit](https://junit.org/)**: Testing framework for unit and integration tests.
- **[Lombok](https://projectlombok.org/)**: Reduces the time of writing Java boilerplate code.
- **[Caching Strategy](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/CacheControl.html)**: Improves efficiency by using HTTP caching, reducing redundant requests, and optimizing client-server interactions.
- **[Hypermedia Principle](https://spring.io/guides/gs/rest-hateoas)**: REST API usability by providing navigable links between resources.

## Setup Instructions

### Prerequisites

- JDK 21
- Apache Maven 3.9.9
- Docker

### Installation Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/your-username/Library-Management-System.git
   cd LibraryManagementSystem
   ```
2. **Build the Application**
   ```bash
   mvn clean install
   ```
3. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```
## Run unit tests using Maven
 ```bash
mvn verify
```
## Run integration tests using Docker
 ```bash
mvn -f pom-docker.xml verify
```

## License ⚖️

This project is licensed under the MIT License. For more details, please refer to the file: [LICENSE](LICENSE).

