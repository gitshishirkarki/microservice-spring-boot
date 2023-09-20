# Inventory Management System
## Spring Boot Micro-service

### Prerequisites
- MySQL :
`docker run -p 3307:3306 --name mysql-container -e MYSQL_ROOT_PASSWORD=password -d mysql:8.0`
- Mongodb : 
`docker run --name mongodb mongo:latest`
- Keycloak : 
`docker run -p 8084:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:22.0.3 start-dev`

#### Docker Compose
- The docker compose file is available in root directory as `docker-compose.yml` and the associated environment file is `.env`
- You can use the docker compose as `docker compose up -d` to run
  - Keycloak
  - MySQL
  - MongoDB
- P.S. If you use docker compose you don't need to run the Prerequisites manually.
