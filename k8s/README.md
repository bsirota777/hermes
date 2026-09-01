# Hermes on Kubernetes — starter setup

Scaffold for running the four Hermes services (user/delivery/profile/wallet)
on Kubernetes, fronted by Kong. Sized for local `kind`, not production —
see "Before this is production-ready" below.

## 1. Local cluster

```bash
kind create cluster --config kind-config.yaml --name hermes
```

## 2. Kong Ingress Controller

```bash
kubectl create -f https://raw.githubusercontent.com/Kong/kubernetes-ingress-controller/main/deploy/single/all-in-one-dbless.yaml
```

Then patch the Kong proxy Service to a fixed NodePort matching `kind-config.yaml`:

```bash
kubectl patch svc kong-proxy -n kong -p '{"spec":{"type":"NodePort","ports":[{"port":80,"targetPort":8000,"nodePort":30080}]}}'
```

## 3. Secrets

```bash
cp base/secrets.example.yaml base/secrets.yaml
# edit base/secrets.yaml with real values - JWT_SECRET must match what
# your services currently use, since existing tokens/tests rely on it
```

`base/secrets.yaml` is not meant to be committed — add it to `.gitignore`.

## 4. Build & load images

Your CI (`docker-publish.yml`) already builds and pushes each service to
`ghcr.io/<repo>/<service>` — the manifests in `services/*.yaml` point there
(replace `YOUR_GH_ORG` with your actual org/repo). For local iteration without
pushing to GHCR, build locally and load straight into kind:

```bash
docker build -f user-service/Dockerfile -t hermes/user-service:local .
kind load docker-image hermes/user-service:local --name hermes
# repeat per service, then set image: hermes/<service>:local in the manifest
```

## 5. Apply everything

```bash
kubectl apply -k .
```

Watch it come up:

```bash
kubectl get pods -n hermes -w
```

Postgres needs to be Ready before the four services will successfully
connect (Flyway runs on their startup) — the readiness probe handles the
ordering, but the first boot can take 20-30s.

## 6. Hit it

```bash
curl http://localhost:8000/users/me -H "Authorization: Bearer <token>"
```

## Before this is production-ready

- **Postgres**: this uses a single in-cluster Postgres Deployment with four
  databases for simplicity. For a real deploy, prefer a managed Postgres
  (RDS/Cloud SQL) or at minimum a proper `StatefulSet` with backups — a bare
  `Deployment` + PVC has no failover story.
- **Images**: point `services/*.yaml` at your real GHCR images and tag
  (`:latest` floats — pin to a sha or release tag for anything beyond local
  testing).
- **Ingress paths**: `kong/ingress.yaml`'s path regexes are a best guess
  from what `hermes-frontend`'s `api/client.js` currently calls — double
  check against your actual controller route list.
- **Frontend origin**: `kong/cors-plugin.yaml` only allows the local Vite
  dev origin; add your real deployed frontend URL once it exists (same
  TODO that was open on the Spring-side CORS config).
- **Replicas/HPA**: everything here runs at `replicas: 1`. Bump replicas
  and/or add a `HorizontalPodAutoscaler` per service once you care about
  actual scaling — this is a starting topology, not a scaled one.
- **Migrations job**: each service currently runs Flyway on its own
  startup, which is fine at replicas: 1 but can race if you scale up later
  — worth moving to a dedicated migration `Job`/init container eventually.
