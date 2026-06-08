# Kubernetes

## Build das imagens locais

```powershell
docker build -t remessa/user-service:0.1 ./user-service
docker build -t remessa/remittance-service:0.1 ./remittance-service
```

## Aplicar manifests

```powershell
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/remittance-service.yaml
```

## Acessar localmente

```powershell
kubectl port-forward service/user-service 8081:8081
kubectl port-forward service/remittance-service 8082:8082
```

Endpoints:

- `user-service`: `http://localhost:8081/api`
- `remittance-service`: `http://localhost:8082/api`

## Remover recursos

```powershell
kubectl delete -f k8s/remittance-service.yaml
kubectl delete -f k8s/user-service.yaml
```
