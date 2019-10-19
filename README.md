# Licenta-scraper

Build the latest image
```
mvn clean compile package
docker build -t gcr.io/bookworm-221210/scraper:latest .
docker push gcr.io/bookworm-221210/scraper:latest
```

Locally deploy
```
minikube start --vm-driver hyperv --hyperv-virtual-switch "Minikube"
```


Deploy the scraper app
```
gcloud container clusters create scraper-cluster
kubectl run scraper --image gcr.io/bookworm-221210/scraper:latest --port 8080
kubectl expose deployment scraper --type LoadBalancer --port 80 --target-port 8080
```
