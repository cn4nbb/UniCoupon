# Repository Guidelines

## Project Structure & Module Organization

UniCoupon is a JDK 17 / Spring Boot 3 coupon system built as a Maven multi-module project. Each module keeps source in `src/main/java`, tests in `src/test/java`, and resources in `src/main/resources`. All code lives under the `edu.cnan.unicoupon.<module>` package root.

- `framework` — shared base: unified results, error codes, exception handling, idempotency, web auto-configuration.
- `gateway` — Spring Cloud Gateway routing, request logging, rate limiting.
- `merchant-admin` — coupon template and delivery-task management, XXL-Job scheduled scanning.
- `engine` — coupon lookup, listing, locking, and redemption.
- `distribution` — batch coupon delivery via RocketMQ and EasyExcel.
- `settlement` — order settlement amount calculation.
- `search` — user coupon search backed by Elasticsearch.

`format/` holds the Eclipse formatter profile; `copyright/` holds the Spotless license header.

## Build, Test, and Development Commands

Use the Maven wrapper from the repository root:

- `./mvnw clean install -DskipTests` — build all modules.
- `./mvnw test` — run tests.
- `./mvnw spotless:apply` — format Java code and apply license headers (also runs during compile).
- `./mvnw spring-boot:run -pl <module>` — run a single service; start order: `framework` (dependency) → `gateway` → `merchant-admin` → `engine` → `distribution` → `settlement` → `search`.

## Coding Style & Naming Conventions

- Use 4-space indentation and a 200-character line limit per `format/uni-coupon_spotless_formatter.xml`.
- Follow the Eclipse formatter profile and Spotless license header from `copyright/copyright.txt`.
- Name classes in PascalCase, methods and variables in camelCase. Use the established suffixes: `XxxDO` for entities, `XxxReqDTO`/`XxxRespDTO` for DTOs, `XxxController`, `XxxService` with implementations under `service/impl`.
- Prefer Lombok for boilerplate; keep `lombok.config` unchanged.
- Manage dependency versions in the root `pom.xml` `dependencyManagement`.

## Testing Guidelines

- Use JUnit 5 (`org.junit.jupiter.api`) with `@SpringBootTest` for integration tests; the existing suite lives mainly in `merchant-admin/src/test/java`.
- Name test classes `XxxTest`/`XxxTests` and methods `testXxx` followed by the scenario.
- Run tests with `./mvnw test`. Tests that touch MySQL, Redis, RocketMQ, or Nacos require those services to be running with local (gitignored) config files.

## Commit & Pull Request Guidelines

History mixes free-form Chinese messages with Conventional Commits; prefer Conventional Commits (`feat:`, `fix:`, `chore:`) with concise descriptions, e.g. `chore: 移除敏感配置文件并更新 .gitignore`.

For pull requests, describe the change and motivation, link the related issue, list tests run, and include screenshots for UI or API-doc changes.

## Security & Configuration Tips

`**/application.yaml` and `**/shardingsphere-config.yaml` are gitignored because they hold database and middleware credentials; create local copies and never commit real secrets. Avoid committing `.log/`, `tmp/`, or IDE workspace files.
