# Auth Service

Microservice xác thực và phân quyền trong hệ thống thương mại điện tử. Tích hợp với **Keycloak** làm Identity Provider (IdP), cung cấp các endpoint đăng ký, đăng nhập. Phân quyền RBAC được thực thi tại **API Gateway**.

---

## Tổng quan kiến trúc

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       Hệ thống Authentication                           │
│                                                                         │
│   Client                                                                │
│     │                                                                   │
│     │  POST /api/auth/register|login|refresh                           │
│     ▼                                                                   │
│  Auth Service (port 8086)                                               │
│     │  Keycloak Admin Client (client_credentials)                       │
│     │──────────────────────────────────────────► Keycloak (port 8080)  │
│     │                     realm: linhlt-2026                            │
│     │                     client: backend                               │
│                                                                         │
│   Client                                                                │
│     │                                                                   │
│     │  Bearer <JWT>  →  POST /api/v1/orders|products/...               │
│     ▼                                                                   │
│  API Gateway (port 8282)                                                │
│     │  F4.4 – Validate JWT: issuer, signature (JWKS), expiration        │
│     │  F4.5 – RBAC: resource_access.backend.roles → ROLE_USER/ADMIN    │
│     │                                                                   │
│     ├──► order-service   (lb://order-service)                          │
│     └──► product-service (lb://product-service)                        │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Yêu cầu chức năng

| ID   | Mô tả                                                                                   | Trạng thái | Nơi thực thi   |
|------|-----------------------------------------------------------------------------------------|------------|----------------|
| F4.1 | Đăng ký `POST /api/auth/register` — tạo user trên Keycloak realm                       | ✅          | auth-service   |
| F4.2 | Đăng nhập `POST /api/auth/login` — trả JWT từ Keycloak                                 | ✅          | auth-service   |
| F4.3 | Role-based access control: `ROLE_USER` truy cập orders/products; `ROLE_ADMIN` cho admin | ✅          | api-gateway    |

---

## Stack công nghệ

| Thành phần       | Công nghệ                                            |
|------------------|------------------------------------------------------|
| Framework        | Spring Boot 4.0.9, Java 21                           |
| Identity Provider| Keycloak 24                                          |
| Keycloak Client  | `keycloak-admin-client` 24.0.0                       |
| Utilities        | Lombok                                               |
| **API Gateway**  | Spring Cloud Gateway + OAuth2 Resource Server        |
| JWT Validation   | `spring-boot-starter-oauth2-resource-server` (JWKS)  |
| Service Registry | Netflix Eureka Client                                |

---

## API Endpoints

### Auth Service `/api/auth`

| Method | Path                  | Mô tả                                             | Auth yêu cầu |
|--------|-----------------------|---------------------------------------------------|--------------|
| `POST` | `/api/auth/register`  | Đăng ký user mới trên Keycloak realm              | Không        |
| `POST` | `/api/auth/login`     | Đăng nhập, nhận JWT access token + refresh token  | Không        |


## Khởi chạy

### Yêu cầu
- Java 21+
- Maven 3.9+
- Keycloak 24+ đang chạy tại `http://localhost:8080`
- Realm `linhlt-2026` và client `backend` đã được cấu hình

### Chạy local
```bash
# Build
mvn clean install -DskipTests

# Chạy
mvn spring-boot:run
```
Auth Service khởi động tại **http://localhost:8086**