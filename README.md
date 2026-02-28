# Propagation POC (traceparent required)

## Behavior
- Call Service A with header `traceparent`.
- Service A forwards the same `traceparent` to Service B.
- Service B returns 400 if missing, 200 if present (echoes the header).
- Service A returns the same status/body as Service B.
- Service A is exposed via LoadBalancer (internet in AKS), Service B is internal.

## Build & Push (Docker Hub)
Replace YOUR_DOCKERHUB_USER.

```bash
docker login

# Build/push B
cd service-b
docker build -t YOUR_DOCKERHUB_USER/service-b:0.0.1 .
docker push YOUR_DOCKERHUB_USER/service-b:0.0.1

# Build/push A
cd ../service-a
docker build -t YOUR_DOCKERHUB_USER/service-a:0.0.1 .
docker push YOUR_DOCKERHUB_USER/service-a:0.0.1
