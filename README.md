# quarkus-vs-springboot-reactive-rest-api
Comparison between `Quarkus and Spring Boot` using a simple Reactive RestFul Api.

## Contents
 1. [Requirements](#requirements)
 2. [Build](#build-and-run)
 3. [Swagger UI](#swagger-ui)
 4. [Quarkus vs Spring Boot Comparision](#quarkus-vs-spring-boot-comparision)
 5. [Naive Stress Test](#naive-stress-testing-curl)
 6. [References](#references)

## Requirements

To compile and run this project you will need:

- JDK 8+
- Mongodb
- Maven 3.6.3
- Keystore(or use `auth profile` to generate one automatic)

## Build and Run

On the root folder need to compile projects

```
mvn clean compile
```

 - #### Run 
If you are not using `auth profile` you will need to place a public key in `/tmp/publicKey.pem` or setting path `PUBLIC_KEY_PATH`.

 
 - #### Quarkus in dev mode
Quarkus has a nice `hot deployment feature` that you can easily develop `on the fly` without need to restart server to recompile the classes, it's a common feature for interpreted languages like `PHP/Python`.

```
cd quarkus
mvn compile quarkus:dev
```

 - ### Using auth profile

If you choose `auth profile` it will generate a keystore if not specified in `PRIVATE_KEY_PATH and PUBLIC_KEY_PATH` also expose endpoint `/api/auth`.

```
QUARKUS_PROFILE=auth mvn compile quarkus:dev

or

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=auth"
```
PS: To change default port(`8081`) set `QUARKUS_HTTP_PORT`.

 - ### Spring Boot

Spring Boot has also `hot deployment feature` but need some interaction from the IDE, more details look at [Hot Swapping](https://docs.spring.io/spring-boot/docs/2.0.x/reference/html/howto-hotswapping.html).

```
cd spring-boot
mvn spring-boot:run
```
PS: To change default port(`8080`) set `mvn spring-boot:run -Dspring-boot.run.arguments="--server.port={PORT}"`.

## Swagger UI

To access [Swagger UI](http://localhost:8080/swagger-ui) and generate a valid JWT use `/api/auth` when `auth profile` is on.

Use following roles:
- `ROLE_ADMIN` - Access for all endpoints
- `ROLE_COMPANY_READ` - Read Access to `GET - /api/companies` and `GET - /api/companies/{id}`.
- `ROLE_COMPANY_CREATE` - Create Access to `POST - /api/companies`
- `ROLE_COMPANY_SAVE` - Update Access to `PUT - /api/companies`
- `ROLE_COMPANY_DELETE` - Delete Access to `DELETE - /api/companies`

PS: To generate a JWT first need to `Logout` on Authorize Button.

### Quarkus vs Spring Boot Comparision

 - #### RestFul API Support

 - #### Reactive Programming Support
 
 - #### Mongodb Support
 
 - #### Swagger Support

 - ### Security(JWT)

 - ### Testing

 - ### Metrics(Prometheus)

 - #### Boot time

 - ### Docker Build
   - Quarkus
     
     It has by default a way to build a docker image and a native image.
     To build a docker image:
     ```
     cd quarkus
     mvn clean package -Dquarkus.container-image.build=true
     docker build -f src/main/docker/Dockerfile.jvm -t quarkus .
     ```
     
     To build native image
     ```
     mvn package -Pnative -Dquarkus.native.container-build=true
     docker build -f src/main/docker/Dockerfile.native -t quarkus .
     ```
     PS: A native image didn't work for me.

   - Spring Boot
     
     Spring Boot doesn't have a built-in plugin but can be easily used with jib or fabric8.
     To build a docker image using [fabric8 plugin](https://dmp.fabric8.io/):
     ```
     cd spring-boot
     mvn clean install docker:build
     ```
    
 - ### Cloud Build(GKE)

 - ### Performance

 - ### Final Result

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

- Get
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
- [Prometheus and Grafana](https://www.callicoder.com/spring-boot-actuator-metrics-monitoring-dashboard-prometheus-grafana/)
