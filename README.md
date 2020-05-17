# quarkus-vs-springboot-reactive-rest-api
Comparison between `Quarkus and Spring Boot` using a simple Reactive RestFul Api.

## Contents
 1. [Requirements](#requirements)
 2. [Build](#build-and-run)
 3. [Swagger UI](#swagger-ui)
 4. [Quarkus vs Spring Boot Comparision](#quarkus-vs-spring-boot-comparision)
 5. [Docker Compose](#docker-compose)
 6. [Naive Stress Test](#naive-stress-testing-curl)
 7. [References](#references)

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
PS: To change default port(`8081`) set `QUARKUS_HTTP_PORT`.

 - ### Using auth profile

If you choose `auth profile` it will generate a keystore if not specified in `PRIVATE_KEY_PATH and PUBLIC_KEY_PATH` also expose endpoint `/api/auth`.

```
QUARKUS_PROFILE=auth mvn compile quarkus:dev

or

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=auth"
```

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
- Quarkus

Quarkus has support for `JAX-RS` with [RESTEasy framework](https://resteasy.github.io/) also `Spring Web Annotations`.
```java
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/example")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExampleResource { 
    @GET
    public Response get() {return Response.ok().entity(something).build(); }     
}     
```

- Spring Boot

Spring also supports `JAX-RS` but more commonly used is with `Spring Web Annotations`.
```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/example")
public class ExampleController {
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> get() {
        return ResponseEntity.ok(something);
    }
}
```
     
 - #### Reactive Programming Support
- Quarkus

Quarkus is reactive by default using [Vert.x](https://vertx.io/) under the hood, the reactive rest support is using [Mutiny](https://github.com/smallrye/smallrye-mutiny) also has support for others libraries like `RxJava and Reactor`.
```java
public class CompanyResource {
    public Multi<Company> getAllActiveCompanies() {
        return Company.streamAll();
    }     

    public Uni<Response> getById(@PathParam("id") String id) {
         return Company.findById(new ObjectId(id))
             .onItem().ifNull().failWith(NotFoundException::new)
             .map(c -> Response.ok(c).build());         
    }
}
```
Full example look at [CompanyResource](quarkus/src/main/java/com/github/quarkus/CompanyResource.java)

- Spring Boot

Spring uses [Reactor](https://projectreactor.io/) for reactive programming but also other libraries like `RxJava`.
```java
public class CompanyController {
    public Multi<Company> getAllActiveCompanies() {
        return Company.streamAll();
    }     

    public Uni<Response> getById(@PathParam("id") String id) {
        return Company.findById(new ObjectId(id))
              .onItem().ifNull().failWith(NotFoundException::new)
              .map(c -> Response.ok(c).build());         
    }
}
```
Full example look at [CompanyController](spring-boot/src/main/java/com/github/springboot/controller/CompanyController.java)     
    
 - #### Mongodb Reactive Support
- Quarkus

Quarkus uses [Panache](https://quarkus.io/guides/mongodb-panache) and provides `Active Record Pattern style and repository`, it's a nice library but has a `preview` and is not backward compatibility.
```java
@MongoEntity(collection = "quarkus_companies")
public class Company extends ReactivePanacheMongoEntity implements Serializable {
    @NotBlank
    public String name;
    
    public static Multi<Company> findActiveCompanies(Integer pageSize) {
        return find("activated", true)
              .page(Page.ofSize(pageSize))
              .stream();
    }
}
```

- Spring Boot

Spring uses [Spring Data](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#mongo.reactive.repositories) and `repository style` but also support other libraries like `RxJava`.
```java
@Repository
public interface CompanyRepository extends ReactiveMongoRepository<Company, String> {
    @Query("{'activated': true}")
    Flux<Company> findActiveCompanies(final Pageable page);
}
```

 - #### Swagger Support

 - ### Security(JWT)

 - ### Testing

 - ### Metrics(Prometheus)

 - ### Docker Build
- Quarkus
     
It has by default support to build a docker image and a native image also.
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
PS: The native image didn't work for me using laptop `i7 16GB dell inpiron`.

- Spring Boot

Spring Boot doesn't have a built-in docker image but can easily use `jib or fabric8`.
To build a docker image using [fabric8 plugin](https://dmp.fabric8.io/):
```
cd spring-boot
mvn clean install docker:build
```
    
 - ### Cloud Build(GKE)

 - ### Performance Test
I've used 2 replicas for each service and deployed pods to GKE.  
 
 - #### Boot Time
Quarkus is really fast to startup as you can see in attached images below, it took less than 5 seconds.
Replica 1
![Replica_1](docs/quarkus_startup_1.png)

Replica 2
![Replica_2](docs/quarkus_startup_2.png)

Spring Boot is not so fast as Quarkus but also not too slow it took average 23 seconds to startup.
Replica 1
![Replica_1](docs/springboot_startup_1.png)

Replica 2
![Replica_2](docs/springboot_startup_2.png)

 - #### Stress 1000 tps per sec
 - Quarkus was slower than Spring Boot to handle the requests but I believe if I change the connection pool would perform better.
![Performance_1](docs/quarkus_performance_test_1.png)
![Performance_2](docs/quarkus_performance_test_2.png)
![Performance_3](docs/quarkus_performance_test_3.png)  

 - Spring Boot handle the requests faster
![Performance_1](docs/springboot_performance_test_1.png)
![Performance_2](docs/springboot_performance_test_2.png)
![Performance_3](docs/springboot_performance_test_3.png)  

 - ### Final Results

### Docker Compose

Once [build the docker images](#docker-build) you can use `docker-compose` to run both services with `mongodb, prometheus and grafana`.

```shell script
cd docker-compose
docker-compose up -d
```

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
