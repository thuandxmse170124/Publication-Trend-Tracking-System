# Project Memory Index

## Architecture
- Spring Boot Backend with SQL Server.
- Hibernate/JPA for ORM.
- **Data Sync Engine**: Built a massive data syncer (`SyncServiceImpl.java`) utilizing OpenAlex API (`/works`).
- Supports deep pagination to fetch thousands of records automatically.
- Multi-threading `@Async` enabled in `DatabaseSeeder.java` for zero-downtime startup data seeding.

## Known Gotchas
- When dealing with real-world paper metadata (OpenAlex), some fields might cause `DataException: String or binary data would be truncated`. The sync engine is designed to swallow individual paper parse errors and continue syncing the rest.
- OpenAlex topics missing `description` are saved as `NULL` intentionally to avoid bottlenecking the sync engine.
- `DatabaseSeeder` avoids double-seeding by checking if `COUNT(*) > 0` before initiating the massive fetch.

## Conventions
- Controller naming: `*Controller.java`
- Service interfaces and implementations are in `service/` and `serviceImpl/`.
- DTOs map complex OpenAlex JSON trees before inserting into Hibernate entities.

## Last Session Summary
- Date: 2026-07-03
- What was done: Resolved Mr. Vinh's critique about missing data and topics. Implemented `DatabaseSeeder` and scaled up OpenAlex synchronization. System now automatically seeds 16,000+ papers across 20 global trending topics on a fresh DB. Successfully ran sync and exported `.sql` script for FE integration.
- Open items: Lazy Loading or Cron Job for enriching topic `description` (Future work).
