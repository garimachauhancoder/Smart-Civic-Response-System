# 🏛️ Smart Civic Response System

An enterprise-grade, high-performance **Municipal Governance and Citizen Response Platform** featuring an intelligent **Pure Java 22 REST API Backend** connected seamlessly to a modern, dark glassmorphic citizen & administrative web portal.

![Smart Civic Hero](web/assets/hero.jpg)

---

## 🌟 Key Capabilities

1. **Pure Java 22 Core (Zero External Jar Dependencies)**:
   - Built on Java SE 22's high-throughput multi-threaded HTTP server (`jdk.httpserver`).
   - Custom lightweight, resilient JSON serialization engine (`SimpleJson`).
   - Thread-safe repository with automatic JSON file persistence (`data/civic_issues.json`).
   - Full CORS support for cross-origin integration.

2. **Smart AI Triage Engine (`SmartTriageService`)**:
   - Natural language keyword parsing and heuristic urgency scoring (0–100).
   - Real-time debounced feedback as citizens type their complaint.
   - Automated municipal department routing:
     - 🛣️ *Roads & Highway Infrastructure*
     - 💧 *Water Supply & Sewerage Board*
     - 🗑️ *Solid Waste Management & Sanitation*
     - ⚡ *Public Lighting & Power Grid*
     - 🛡️ *Public Safety & Disaster Relief*
     - 🌳 *Urban Parks & Green Environment*
   - Automated SLA determination: **Critical (12h)**, **High (24h)**, **Medium (48h)**, **Low (72h)**.

3. **Citizen Reporting & Tracking Portal**:
   - Geolocation & GIS coordinate pin-drop map.
   - Evidence photo upload with camera support and preset samples (*Water Burst, Pothole, Garbage, Sparking Cable*).
   - Real-time interactive resolution progress timeline (Reported ➔ Triaged ➔ In Progress ➔ Resolved ➔ Closed).
   - Community endorsement / Upvoting engine that auto-escalates high-impact issues.

4. **Municipal Admin Command Center**:
   - Real-time KPI summary (Total Incidents, Pending Triage, Field Operations, Resolution Rate %, SLA Breaches).
   - Live operations table with 1-click status transitions and nodal officer assignment modal.
   - Department workload directory and contact grid.

---

## 📂 Project Directory Structure

```
Smart_civic_response_system/
├── src/
│   └── com/
│       └── civic/
│           ├── model/
│           │   ├── CivicIssue.java        # Main issue entity with SLA & timeline
│           │   ├── Department.java        # Municipal agency metadata
│           │   ├── IssueStatus.java       # Lifecycle states (REPORTED -> CLOSED)
│           │   ├── PriorityLevel.java     # Urgency levels (CRITICAL -> LOW)
│           │   └── TimelineEvent.java     # Audit event trail
│           ├── repository/
│           │   └── IssueRepository.java   # Concurrent storage & JSON disk persistence
│           ├── service/
│           │   ├── CivicIssueService.java # Business workflows & analytics
│           │   └── SmartTriageService.java# NLP keyword analyzer & SLA calculator
│           ├── server/
│           │   ├── CivicHttpServer.java   # Multi-threaded Java HTTP daemon
│           │   ├── RestApiHandler.java    # REST API routing & JSON controllers
│           │   ├── StaticFileHandler.java # Web asset & MIME dispatcher
│           │   └── CorsHelper.java        # Cross-Origin resource sharing headers
│           ├── util/
│           │   └── SimpleJson.java        # Pure Java JSON serializer/deserializer
│           └── Main.java                  # Application entry point
├── web/
│   ├── index.html                         # Modern Single-Page Application (SPA)
│   ├── css/
│   │   └── style.css                      # Obsidian glassmorphic design system
│   ├── js/
│   │   ├── api.js                         # REST API async client wrapper
│   │   └── app.js                         # Event handlers, map pin & UI state
│   └── assets/
│       └── hero.jpg                       # High-tech city command center banner
├── data/
│   └── civic_issues.json                  # Persistent JSON incident storage
├── pom.xml                                # Standard Maven configuration
├── requirements.txt                       # Project runtime & client test specs
├── test_civic_api.py                      # Automated REST API test client
├── run.bat                                # Windows 1-click batch launcher
├── run.ps1                                # PowerShell launcher script
└── README.md                              # Complete system documentation
```

---

## 🚀 Quick Start & Launch Instructions

### 1. Launch with Windows Batch Script
Double-click `run.bat` or run:
```cmd
run.bat
```

### 2. Launch with PowerShell
```powershell
.\run.ps1
```

### 3. Manual Compilation & Execution
```cmd
javac -d bin -sourcepath src src\com\civic\Main.java
java -cp bin com.civic.Main
```

### 4. Access the Application
- **Citizen & Admin Web Portal:** [http://localhost:8080](http://localhost:8080)
- **API Health Check:** [http://localhost:8080/api/health](http://localhost:8080/api/health)
- **REST Issues API:** [http://localhost:8080/api/issues](http://localhost:8080/api/issues)
- **Analytics & KPIs:** [http://localhost:8080/api/stats](http://localhost:8080/api/stats)
- **Department Network:** [http://localhost:8080/api/departments](http://localhost:8080/api/departments)

---

## 📡 REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Service health status and uptime verification |
| `GET` | `/api/stats` | Real-time municipal KPIs, SLA breaches, resolution rate |
| `GET` | `/api/issues` | Query issues with optional filters (`category`, `status`, `priority`, `q`) |
| `POST` | `/api/issues` | Register a new civic complaint with automated AI triage |
| `GET` | `/api/issues/{id}` | Detailed ticket view including complete progress timeline |
| `PUT` | `/api/issues/{id}/status` | Transition issue status (`IN_PROGRESS`, `RESOLVED`), assign officer |
| `PUT` | `/api/issues/{id}/assign` | Reassign issue to a different municipal department |
| `POST` | `/api/issues/{id}/upvote` | Community citizen endorsement (+1 vote and priority escalation) |
| `POST` | `/api/triage` | AI text triage analysis returning predicted category & SLA |
| `GET` | `/api/departments` | Directory of municipal departments and active officer loads |

---

## 🧪 Automated API Testing

A Python test suite is provided in `test_civic_api.py` that exercises the endpoints. To execute:
```bash
python test_civic_api.py
```
*(Dependencies are documented in `requirements.txt`).*
