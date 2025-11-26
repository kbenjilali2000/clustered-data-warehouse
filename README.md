# FX Deals Clustered Data Warehouse  
**ProgressSoft – Technical Assignment (Java Developer)**

---

## Overview

Clustered Data Warehouse – FX Deals Import Service is a Spring Boot application designed to ingest and validate Foreign Exchange (FX) deal records coming from:  
- JSON array requests  
- CSV uploaded files  

The system performs per-row validation, identifies invalid and duplicate deals, persists valid deals into PostgreSQL, and returns a detailed import summary.  
This project demonstrates strong code architecture, clean separation of concerns, proper validation, robust error handling, and production-ready logging.

---


## Features

| Feature                           | Description                                                                            |
|-----------------------------------|----------------------------------------------------------------------------------------|
| **JSON Batch Import**             | `POST /api/deals/import` accepts JSON arrays; validates and imports deals              |
| **CSV File Import**               | `POST /api/deals/import/csv` accepts multipart CSV; parses, validates, reports errors  |
| **No Rollback Strategy**          | Each row processed independently in an append-only manner                              |
| **Idempotency**                   | Duplicate Deal IDs ignored and reported; DB unique constraints enforce                 |
| **Validation Layers**             | Bean Validation, business logic, CSV parsing checks                                    |
| **Global Exception Handling**     | @RestControllerAdvice for consistent JSON API error structures                         |
| **Docker Support (coming)**       | Dockerfile and docker-compose configurations for easy deployment                       |

---

## Architecture
<img width="632" height="977" alt="image" src="https://github.com/user-attachments/assets/99c95775-d3a3-4712-8cb0-3c1d5c6bca7d" />

---

## Testing

### Included Tests

| Test Case            | Description                           |
|----------------------|-------------------------------------|
| Valid deal import    | Saves one valid deal                 |
| Duplicate detection  | Duplicate rows skipped               |
| Invalid business rule| Invalid when currencies match        |
| Null or malformed deal| Logged as invalid                    |
| Mixed valid + invalid + duplicate | Summary calculated correctly   |
| DB exception        | Handled gracefully without crashing  |
<img width="930" height="342" alt="image" src="https://github.com/user-attachments/assets/33cd76a7-0841-4a0f-953f-b8640d5f4489" />

---

## Database

**Table Schema (created automatically via JPA/Hibernate):**
<img width="1710" height="945" alt="image" src="https://github.com/user-attachments/assets/363802a5-ebbf-454c-b20e-880941de1037" />


---

## Technologies Used

| Technology       | Description                                 |
|------------------|---------------------------------------------|
| Java 17          | Main programming language                    |
| Spring Boot 3.x  | Backend framework                            |
| Spring Data JPA  | ORM and repository abstraction               |
| Hibernate        | JPA implementation                          |
| PostgreSQL       | Persistent relational database                |
| Maven            | Build automation tool                       |
| Lombok           | Boilerplate reduction for DTOs & entities   |
| SLF4J / Logback  | Logging                                     |
| Docker / Compose | Containerization & service orchestration (planned) |

---

## API Endpoints

### 1. Import JSON Batch  
`POST /api/deals/import`  

**Request Body Example:**
<img width="937" height="361" alt="image" src="https://github.com/user-attachments/assets/65364d49-f794-48ca-b1fa-cc1a015c944c" />

**Response Example:**
<img width="1249" height="246" alt="image" src="https://github.com/user-attachments/assets/c31dba99-0f2d-45ab-97a9-a2d0c87bf419" />
<img width="952" height="343" alt="image" src="https://github.com/user-attachments/assets/44e08c36-7353-49fd-8f41-b3a5ccbe506c" />

---

### 2. Import CSV File  
`POST /api/deals/import/csv`  

**Upload example with Postman (form-data):**
<img width="1033" height="215" alt="image" src="https://github.com/user-attachments/assets/408e6816-9c16-423f-8766-c65d8328ea6f" />
<img width="838" height="192" alt="image" src="https://github.com/user-attachments/assets/1455f636-b097-4c68-8383-6d1c692a0a0f" />

**Response Example:**
<img width="947" height="345" alt="image" src="https://github.com/user-attachments/assets/18a2ca86-5716-47b6-a26a-c1b3ae982651" />
<img width="1261" height="361" alt="image" src="https://github.com/user-attachments/assets/d87ad465-93e3-4a52-ae65-a93b58272cec" />


## Author

**Khadija Benjilali**  
Java Developer | ProgressSoft Technical Assignment (2025)  

---

## License

This project is for educational and technical assignment purposes only.

---

**Ready to proceed with Docker setup and containerized deployment? Just say "Let's start Docker".**


See all Docker files and instructions in the branch:  
**`features/dockerisation`**
---



