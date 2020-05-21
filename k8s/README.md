### Create Secrets for Keystore files

```
kubectl create secret generic privatekey --from-file=../docker-compose/dummy_privateKey.pem

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

### Using Minikube with Istio

- Install

```shell script
curl -Lo minikube https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64   && chmod +x minikube
```
PS: Virtual as pre-requirement

 - Start
  
```
minikube start --memory=8192 --cpus=4 --kubernetes-version=v1.13.1     --extra-config=controller-manager.cluster-signing-cert-file="/var/lib/minikube/certs/ca.crt"     --extra-config=controller-manager.cluster-signing-key-file="/var/lib/minikube/certs/ca.key"     --vm-driver=virtualbox
```

 - Docker-env
 
```shell script
eval $(minikube docker-env)
```
Follow instructions https://stackoverflow.com/questions/42564058/how-to-use-local-docker-images-with-minikube

 - Rebuild images
Follow instructions https://github.com/rodrigorodrigues/quarkus-vs-springboot-reactive-rest-api#docker-build
PS: All commands should apply in the same `terminal console` or setting docker-env for each terminal console.

- Check if pods are running

```shell script
kubectl get pods --show-labels
```

- Accessing pods by `port-forward`
```shell script
# Spring Boot
kubectl port-forward $(kubectl get pod --selector="app=spring-boot" --output jsonpath='{.items[0].metadata.name}') 8080:8080

# Quarkus
kubectl port-forward $(kubectl get pod --selector="app=quarkus" --output jsonpath='{.items[0].metadata.name}') 8081:8081
```
It should open access to http://localhost:8080/swagger-ui.html and for Quarkus port `8081` to call endpoints.

 - Istio - WIP(not working yet)
Follow demo installation for the official link(https://istio.io/docs/setup/getting-started/#download).
