# quarkus-vs-springboot-reactive-rest-api
Comparison between `Quarkus and Spring Boot` using a simple Reactive RestFul Api.

## Requirements

To compile and run this project you will need:

- JDK 8+
- Mongodb
- Maven 3.6.3
- Keystore(or use `auth profile` to generate one automatic)

## Build

```
mvn clean package
```

## Run `Quarkus` with dev mode

You will need to place a public key in `/tmp/publicKey.pem` or change path setting `PUBLIC_KEY_PATH`.

```
cd quarkus
mvn compile quarkus:dev
```

## Run `Quarkus` with auth profile

It will generate a keystore and configure to service.

```
QUARKUS_PROFILE=auth mvn compile quarkus:dev
```
PS: To change default port(`8081`) set `QUARKUS_HTTP_PORT`.

## Run `Spring Boot`

You will need to place a public key in `/tmp/publicKey.pem` or change path setting `PUBLIC_KEY_PATH`.

```
cd spring-boot
mvn spring-boot:run
```
PS: To change default port(`8080`) set `-Dspring-boot.run.arguments="--server.port={PORT}"`.

## Swagger UI

To access [Swagger UI](http://localhost:8080/swagger-ui) and generate a valid JWT use `/api/auth` when `auth profile` is on.

Use following roles:
- ROLE_ADMIN - Access for all endpoints
- ROLE_COMPANY_READ - Read Access to `GET - /api/companies` and `GET - /api/companies/{id}`.
- ROLE_COMPANY_CREATE - Create Access to `POST - /api/companies`
- ROLE_COMPANY_SAVE - Update Access to `PUT - /api/companies`
- ROLE_COMPANY_DELETE - Delete Access to `DELETE - /api/companies`

PS: To generate a JWT first need to `Logout` on Authorize Button.

### Boot time

### Development Mode

### Swagger Support

### Security(JWT)

### Testing

### Metrics

### Docker Build

### Cloud Build(GKE)

### Performance

### Naive Stress Testing cURL
- Create
```shell script
./naive-stress-test.sh -c 1 -r 1 \
-a localhost:8080/api/companies \
-X POST \
-H "Authorization: bearer XXXX" \
-H "Content-Type: application/json" \
-d '{"name": "test curl", "activated": true}'
```

- Create appending body with microseconds
```shell script
./naive-stress-test.sh -c 1 -r 1 \
-a localhost:8080/api/companies \
-X POST \
-H "Authorization: bearer XXXX" \
-H "Content-Type: application/json" \
-d '{"name": "test curl - #INCREMENT_TIMESTAMP", "activated": true}'
```

-Get
```shell script
./naive-stress-test.sh -c 1 -r 1 \
-a localhost:8080/api/companies \
-X GET \
-H "Authorization: bearer XXXX" \
-H "Content-Type: application/json"
```
### References
- [WebFlux Security](https://docs.spring.io/spring-security/site/docs/current/reference/html5/#jc-webflux)
- [Read Private/Public Key](https://gist.github.com/destan/b708d11bd4f403506d6d5bb5fe6a82c5)
- [Quarkus Rest](https://quarkus.io/guides/rest-json)
- [Quarkus JWT](https://quarkus.io/guides/security-jwt#generating-a-jwt)
- [Quarkus MongoDb Pinache](https://quarkus.io/guides/mongodb-panache)
- [Quarkus Reactive](https://quarkus.io/guides/getting-started-reactive#mutiny)
- [Mutiny Reactive](https://smallrye.io/smallrye-mutiny/)
- [Generate RSA Keys](https://www.novixys.com/blog/how-to-generate-rsa-keys-java/)
- [Quarkus vs Spring Boot](https://dzone.com/articles/microservices-quarkus-vs-spring-boot)
- [MongoDB Convert to Capped Collection](https://stackoverflow.com/questions/7904526/how-to-create-a-capped-collection-with-spring-data-mongodb)
- [Stress Test cURL](https://gist.github.com/cirocosta/de576304f1432fad5b3a)
