# П15 — SSL/TLS: Nginx reverse proxy для production

**Статус:** 🔲 Не выполнено  
**Источник:** П14.4, выделен отдельно как инфраструктурная задача

---

## Проблема

Трафик между клиентом и приложением передаётся по HTTP — JWT-токены и данные открыты для перехвата.
Spring Boot не имеет настроенного SSL. Необходим HTTPS-терминатор перед приложением.

---

## Решение: Nginx + Let's Encrypt

### 1. `docker/nginx/nginx.conf`

```nginx
server {
    listen 80;
    server_name ${DOMAIN};
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ${DOMAIN};

    ssl_certificate     /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;

    location / {
        proxy_pass         http://app:8081;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }
}
```

### 2. `docker/docker-compose.prod.yml` — добавить nginx-сервис

```yaml
services:
  nginx:
    image: nginx:1.27-alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
    depends_on:
      - app
    restart: unless-stopped
```

### 3. `src/main/resources/application-prod.yml` — доверять X-Forwarded-Proto

```yaml
server:
  forward-headers-strategy: native
```

### 4. Certbot на сервере (разовая настройка)

```bash
apt install certbot
certbot certonly --standalone -d your-domain.com
# обновление раз в 90 дней (автоматически через systemd timer)
```

---

## Чеклист

- [ ] Создать `docker/nginx/nginx.conf`
- [ ] Добавить nginx-сервис в `docker/docker-compose.prod.yml`
- [ ] Добавить `forward-headers-strategy: native` в `application-prod.yml`
- [ ] Настроить Certbot на сервере и получить сертификат
- [ ] Убедиться, что порт 443 открыт в firewall
- [ ] Проверить редирект HTTP → HTTPS
- [ ] Проверить HSTS-заголовок в ответе (`Strict-Transport-Security`)

---

## Заметки

- HSTS уже настроен в `SecurityConfig` (`max-age=31536000; includeSubDomains; preload`) — заработает после включения HTTPS
- `forward-headers-strategy: native` нужен, чтобы Spring корректно видел `https` в `X-Forwarded-Proto` от Nginx
- Не использовать `allowedOrigins("*")` в CORS — origins уже управляются через `DEVCREW_CORS_ALLOWED_ORIGINS`
