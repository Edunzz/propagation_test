# Propagation POC (traceparent required) — Service A -> Service B

POC en Kubernetes con **2 servicios Java**:

- **Service A** expone `/call`
  - Requiere header **`traceparent`** en la llamada entrante
  - Reenvía **tal cual** ese `traceparent` a **Service B**
  - Responde con **el mismo status + body** que devuelve B
- **Service B** expone `/receive`
  - Si **NO** recibe `traceparent` → **400**
  - Si lo recibe → **200** y devuelve un mensaje con el `traceparent`

> Namespace usado: `propagation-test` (en Kubernetes no se permiten espacios en el nombre del namespace).

---

## 1) Requisitos

- Docker instalado
- Acceso a tu cluster Kubernetes (AKS) y `kubectl` configurado
- Cuenta en Docker Hub

---

## 2) Estructura del repo

```text
propagation-poc/
  service-a/
    Dockerfile
    pom.xml
    src/main/java/demo/a/ServiceAApplication.java
    src/main/java/demo/a/AController.java
  service-b/
    Dockerfile
    pom.xml
    src/main/java/demo/b/ServiceBApplication.java
    src/main/java/demo/b/BController.java
  k8s/
    propagation-test.yaml
  README.md
```

---

## 3) Build & Push (Docker Hub)

Reemplaza `YOUR_DOCKERHUB_USER` por tu usuario Docker Hub.

```bash
docker login

# --- Service B ---
cd service-b
docker build -t YOUR_DOCKERHUB_USER/service-b:0.0.1 .
docker push YOUR_DOCKERHUB_USER/service-b:0.0.1

# --- Service A ---
cd ../service-a
docker build -t YOUR_DOCKERHUB_USER/service-a:0.0.1 .
docker push YOUR_DOCKERHUB_USER/service-a:0.0.1

cd ..
```

---

## 4) Deploy en Kubernetes (AKS)

1) Edita `k8s/propagation-test.yaml` y reemplaza:

- `image: YOUR_DOCKERHUB_USER/service-b:0.0.1`
- `image: YOUR_DOCKERHUB_USER/service-a:0.0.1`

2) Aplica el manifiesto:

```bash
kubectl apply -f k8s/propagation-test.yaml
kubectl get all -n propagation-test
```

3) Espera la IP pública del LoadBalancer de Service A:

```bash
kubectl get svc -n propagation-test service-a -w
```

Cuando veas `EXTERNAL-IP`, ya puedes probar desde fuera.

---

## 5) Pruebas

### 5.1) Llamar a Service A desde Internet (con traceparent) ✅

```bash
curl -i "http://<EXTERNAL-IP>:8080/call" \
  -H "traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
```

Resultado esperado:
- Status **200**
- Body tipo: `B: OK - received traceparent=...`

### 5.2) Llamar a Service A SIN header (debe fallar) ❌

```bash
curl -i "http://<EXTERNAL-IP>:8080/call"
```

Resultado esperado:
- Status **400**
- Body: `A: Missing required header traceparent`

### 5.3) Probar Service B desde un pod (sin header → 400) ❌

```bash
kubectl run -n propagation-test curlpod --rm -it --image=curlimages/curl -- \
  curl -i "http://service-b:8080/receive"
```

### 5.4) Probar Service B desde un pod (con header → 200) ✅

```bash
kubectl run -n propagation-test curlpod --rm -it --image=curlimages/curl -- \
  curl -i "http://service-b:8080/receive" \
  -H "traceparent: 00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01"
```

---

## 6) (Opcional) Subir este repo a tu GitHub

1) Crea un repo vacío en GitHub, por ejemplo: `propagation-poc`
2) En tu máquina, desde la raíz del proyecto:

```bash
git init
git add .
git commit -m "POC: traceparent propagation A->B"
git branch -M main
git remote add origin https://github.com/<TU_USUARIO>/propagation-poc.git
git push -u origin main
```

---

## 7) Troubleshooting rápido

### 7.1) Service A no obtiene EXTERNAL-IP
- En AKS, `type: LoadBalancer` puede tardar un poco.
- Revisa eventos:

```bash
kubectl describe svc -n propagation-test service-a
```

### 7.2) ImagePullBackOff (si tu repo DockerHub es privado)
- Necesitas `imagePullSecret` y agregarlos al YAML.
