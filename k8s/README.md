### Create Secrets for Keystore files

```
kubectl create secret generic privatekey --from-file=../docker-compose/dummy_privatekeyKey.pem

kubectl create secret generic publickey --from-file=../docker-compose/dummy_publicKey.pem
```

### Pushing Docker images to Google Cloud

Once [docker images are built](https://github.com/rodrigorodrigues/quarkus-vs-springboot-reactive-rest-api#docker-build) you can tag/push docker images to Google Cloud(or another cloud provider).

```
docker tag quarkus:latest eu.gcr.io/YOUR_PROJECT/quarkus:latest
docker push eu.gcr.io/YOUR_PROJECT/quarkus
```

PS: Change `YOUR_PROJECT` to yours

### Deploying Docker images to Google Cloud

```
kubectl apply -f deployment-mongo.yml
kubectl apply -f deployment-spring-boot.yml
kubectl apply -f deployment-quarkus.yml
```
