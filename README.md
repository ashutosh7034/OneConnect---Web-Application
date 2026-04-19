# OneConnect

OneConnect is a Java Servlet based super-app style web application.
It provides a single dashboard to discover and open popular services, save favourites, manage profile data, and get AI-based app suggestions.

## Features

- User authentication (register/login)
- Service dashboard with dynamic catalog
- Search with fuzzy/related-word matching
- Favourites add/remove flow
- Profile update and password change
- AI Guide recommendations with multi-level fallback
- Consistent OneConnect branding and app logo rendering in cards

## Tech Stack

- Java 8 (Servlet/JSP style web app, WAR packaging)
- Maven
- Tomcat via `tomcat7-maven-plugin`
- MySQL
- Gson
- Frontend: HTML, CSS, vanilla JavaScript

## Current Project Structure

- `src/main/java/com/oneconnect/...` backend servlets, DAOs, utilities
- `src/main/resources/services.json` source of truth for app catalog
- `src/main/webapp/*.html` UI pages
- `src/main/webapp/js/*.js` frontend logic
- `src/main/webapp/css/style.css` shared styles
- `src/main/webapp/assets/oneconnect-logo.svg` shared OneConnect logo
- `database.sql` database schema
- `ai_rag_service/` optional Python AI service

## API Endpoints

Servlet mappings currently in use:

- `POST /api/register`
- `POST /api/login`
- `GET /api/services`
- `GET /api/search?q=...`
- `GET /api/favourites?userId=...`
- `POST /api/favourites`
- `DELETE /api/favourites`
- `GET /api/profile?userId=...`
- `PUT /api/profile`
- `PUT /api/profile/password`
- `POST /api/ai-guide`

## Database Setup

1. Start MySQL.
2. Run:

```sql
SOURCE database.sql;
```

3. Verify DB credentials in `src/main/java/com/oneconnect/util/DBConnection.java`.

Current defaults:

- URL: `jdbc:mysql://localhost:3307/oneconnect_db?useSSL=false&serverTimezone=UTC`
- User: `root`
- Password: `root`

## Run the App

From project root:

### Windows (bundled Maven)

```powershell
.\apache-maven-3.9.6\bin\mvn.cmd tomcat7:run
```

### System Maven

```bash
mvn tomcat7:run
```

Open:

`http://localhost:8080/OneConnect`

## Build WAR

```bash
mvn clean package
```

Output:

- `target/OneConnect.war`

## AI Guide Modes

AI Guide uses layered fallback:

1. Python service (`ai_rag_service`) if running
2. Java endpoint `/api/ai-guide`
3. Frontend local recommendation fallback

Run optional Python service:

```bash
cd ai_rag_service
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

## Notes on Service Logos

- Service cards fetch app logos using favicon lookups from each service URL.
- If a favicon is unavailable, the UI shows a graceful initial-letter fallback.

## Frontend Pages

- `index.html` login
- `register.html` signup
- `dashboard.html` service hub
- `search.html` search results
- `favourites.html` saved apps
- `profile.html` profile management
- `ai-guide.html` conversational recommendations

## Troubleshooting

1. Port `8080` already in use
- Stop the process using port `8080`, then restart Maven run.

2. `mvn clean tomcat7:run` fails deleting `target/tomcat/logs/...`
- A stale Java/Tomcat process is likely locking files.
- Stop that process and run again.

3. MySQL connection issues
- Ensure MySQL is running on configured port (`3307` by default).
- Verify credentials in `DBConnection.java`.
- Ensure `oneconnect_db` exists.

4. App shows stale UI/JS
- Hard refresh browser (`Ctrl+F5`) after frontend changes.

## Development Notes

- Servlet routes are annotation-based with `@WebServlet`.
- App context path is `/OneConnect` (from Maven Tomcat plugin config).
- Service catalog is data-driven via `services.json`.
