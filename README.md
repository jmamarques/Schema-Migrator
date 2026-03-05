# Schema Migrator - Oracle to H2

## Quick Start Guide

### 1. Start Oracle Test Database
```bash
docker-compose up -d
```

### 2. Monitor Database Startup
```bash
docker-compose logs -f oracle
```

### 3. Connect to Oracle (Verification)
```bash
docker exec -it oracle-test sqlplus scott/tiger@//localhost:1521/FREEPDB1
```

### 4. Run Schema Migration
Create a `tables.txt` file:
```
EMP;DEPT;BONUS;SALGRADE
```

Execute the migration:
```bash
java -jar target/schema-migrator-1.0.0.jar migrate \
  -i tables.txt \
  --oracle-url jdbc:oracle:thin:@//localhost:1521/FREEPDB1 \
  --oracle-user scott \
  --oracle-password tiger \
  --schema SCOTT \
  -o output.sql
```

### 5. Clean Up
```bash
docker-compose down -v
```

## Output
The generated H2-compatible schema will be saved to `output.sql`