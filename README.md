API Rate Limiter Service

A REST service that limits how many requests each API key can make per minute. Requests over the limit get HTTP 429 Too Many Requests. API keys and request counts are stored in MySQL.

Tech stack: Java 21, Spring Boot, Spring Data JPA, MySQL, Maven

How it works
<img width="855" height="312" alt="image" src="https://github.com/user-attachments/assets/75708418-93ab-4702-bad1-2b995525af6a" />

X-API-Key header
within limit
limit exceeded
missing or invalid key
Client / Postman
RateLimitFilter
RateLimiterService
MySQL: api_keys,request_counts
Endpoint: 200 OK
429 Too Many Requests
401 Unauthorized
The client sends a request with an X-API-Key header.
A servlet filter (RateLimitFilter) looks up the key in MySQL.
The service counts the requests made by that key in the current minute (fixed window algorithm).
If the count is below the key's limit, the request continues. Otherwise the service returns 429 with a Retry-After header.

Response headers on protected endpoints:

Header	Meaning
X-RateLimit-Limit	Requests allowed per minute for this key
X-RateLimit-Remaining	Requests left in the current minute
Retry-After	Seconds until the next window (only on 429)
API endpoints
Method	Endpoint	Description	Rate limited
GET	/api/hello	Demo endpoint protected by the limiter	Yes
POST	/api/keys	Create a new API key	No
GET	/api/usage	Show used and remaining requests (needs X-API-Key)	No
DELETE	/api/keys/{id}	Disable a key	No
Example requests

Create a key:

bash
curl -X POST http://localhost:8080/api/keys \
-H "Content-Type: application/json" \
-d '{"ownerName":"Demo","requestLimit":3}'
json
{
"id": 3,
"apiKey": "19cf5fbb-2b79-46b8-8c5e-ce2a9bd2aa7e",
"ownerName": "Demo",
"requestLimit": 3
}

Call the protected endpoint:

bash
curl -i -H "X-API-Key: YOUR_KEY" http://localhost:8080/api/hello

Check usage:

bash
curl -H "X-API-Key: YOUR_KEY" http://localhost:8080/api/usage
json
{
"ownerName": "Demo",
"limitPerMinute": 3,
"usedThisMinute": 3,
"remaining": 0
}
Status codes
Code	When
200	Request allowed
401	Missing, invalid or disabled API key
429	Limit exceeded for the current minute
Test results

Sending 6 requests in one minute with a key limited to 5 per minute:

200
200
200
200
200
429

Rate limit test

The counters are stored in MySQL:

Request counts table

Database schema
sql
CREATE TABLE api_keys (
id INT AUTO_INCREMENT PRIMARY KEY,
api_key VARCHAR(64) NOT NULL UNIQUE,
owner_name VARCHAR(100) NOT NULL,
request_limit INT NOT NULL DEFAULT 5,
is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE request_counts (
id INT AUTO_INCREMENT PRIMARY KEY,
key_id INT NOT NULL,
window_start DATETIME NOT NULL,
request_count INT NOT NULL DEFAULT 0,
FOREIGN KEY (key_id) REFERENCES api_keys(id),
UNIQUE (key_id, window_start)
);
Project structure
src/main/java/com/ayan/ratelimiter/
├── controller/   ApiKeyController (create key, usage, disable key), HelloController
├── filter/       RateLimitFilter (checks the key on every /api request)
├── model/        ApiKey, RequestCount (JPA entities)
├── repository/   ApiKeyRepository, RequestCountRepository
└── service/      RateLimiterService, RateLimitResult (limiting logic)
Run it locally

Requirements: JDK 21, MySQL 8, Maven (the project includes the Maven wrapper)

Create the database and tables with the schema above:
sql
CREATE DATABASE rate_limiter_db;
USE rate_limiter_db;
-- then run the CREATE TABLE statements from the schema section

INSERT INTO api_keys (api_key, owner_name, request_limit)
VALUES ('abc123', 'Demo', 5);
Create a MySQL user for the app:
sql
CREATE USER 'ratelimiter'@'localhost' IDENTIFIED BY 'choose_a_password';
GRANT ALL PRIVILEGES ON rate_limiter_db.* TO 'ratelimiter'@'localhost';
Copy src/main/resources/application.properties.example to src/main/resources/application.properties and set your password:
spring.datasource.url=jdbc:mysql://localhost:3306/rate_limiter_db
spring.datasource.username=ratelimiter
spring.datasource.password=choose_a_password

application.properties is listed in .gitignore, so your password is never committed.

Start the app:
bash
./mvnw spring-boot:run

On Windows use mvnw.cmd spring-boot:run. The service runs on http://localhost:8080.

Limitations and future improvements
Fixed window algorithm: a client can send a burst at the end of one minute and another at the start of the next. A sliding window or token bucket would smooth this out.
Concurrency: the counter is read, then updated. Under very heavy parallel traffic two requests could be counted at the same time. An atomic update query or Redis would fix this.
No admin authentication: anyone can create keys. A real deployment would protect /api/keys.
Planned: Redis for shared counters across several instances, Docker Compose, JUnit tests for the limiter logic, and a small dashboard.
Author

Ayan Sarguru GitHub: ayansar123 | LinkedIn: ayan-sarguru
