### Pushing Docker images to Cloud Provider

Once [docker images are built](https://github.com/rodrigorodrigues/quarkus-vs-springboot-reactive-rest-api#docker-build) you can tag/push docker images to Google Cloud(or another cloud provider).

```
docker tag quarkus:latest eu.gcr.io/YOUR_PROJECT/quarkus:latest
docker push eu.gcr.io/YOUR_PROJECT/quarkus
```

PS: Change `YOUR_PROJECT` to yours.

### Deploying Pods

```
kubectl apply -f deployment-mongo.yml
kubectl apply -f deployment-spring-boot.yml
kubectl apply -f deployment-quarkus.yml
kubectl apply -f deployment-istio-gateway.yml
```

### Using Minikube with Istio

- Install kubectl

Follow instructions https://kubernetes.io/docs/tasks/tools/install-kubectl/#install-kubectl-on-linux

- Install minikube

Follow instructions https://kubernetes.io/docs/tasks/tools/install-minikube/

 - Start Minikube with Virtualbox

```
minikube start --memory=8192 --cpus=4 --vm-driver=virtualbox
```

- Docker-env
 
```shell script
eval $(minikube docker-env)
```

Follow instructions https://stackoverflow.com/questions/42564058/how-to-use-local-docker-images-with-minikube

PS: All commands should be in the same `terminal console` or setting `docker-env command` for each terminal console.

 - Create Secrets

```
kubectl create secret generic privatekey --from-file=../docker-compose/dummy_privateKey.pem

kubectl create secret generic publickey --from-file=../docker-compose/dummy_publicKey.pem
```

 - Rebuild images
 
Follow instructions https://github.com/rodrigorodrigues/quarkus-vs-springboot-reactive-rest-api#docker-build

- Deploying pods

Check [previous](#deploying-pods) section.

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

- Istio

Follow instructions https://istio.io/docs/setup/getting-started/

```shell script
# Download latest version
curl -L https://git.io/getLatestIstio | sh -

# Add istioctl to classpath
export PATH=$PWD/bin:$PATH

# Install istio
istioctl manifest apply --set profile=demo

# Check list of pods on istio-system namespace
kubectl get pods --show-labels --namespace istio-system

# Export Ingress host
export INGRESS_HOST=$(minikube ip)

# Export Port
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')

# Export Gateway Url
xport GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT

# Call api
curl -H "Authorization: bearer XXXX" -H "Content-Type: application/json" -v https://$GATEWAY_URL/api/companies
```
