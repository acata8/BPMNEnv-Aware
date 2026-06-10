# Camunda Location-Aware Workflow System

![CI](https://github.com/acata8/BPMNEnv-Aware/actions/workflows/ci.yml/badge.svg)

BPMN workflow platform where process execution reacts to real-time
location data: GPS tracking over WebSocket, geofencing, and dynamic
routing handled by an embedded Camunda BPM engine.

Developed as MSc thesis project — University of Camerino, 2025.
BPMN diagrams are modeled with a [custom Camunda plugin](https://github.com/acata8/BPMNEnv-Aware-Plugins)

## Architecture

- **Backend**: Java 17, Spring Boot, embedded Camunda BPM engine
- **Real-time tracking**: WebSocket channel for live GPS coordinate
  ingestion, with geofencing logic driving process routing
- **Process correlation**: instances linked via business key and
  participant ID

## CI/CD

- Build and tests on every push/PR via a **reusable GitHub Actions
  workflow** (Maven, JDK 17)
- **Automated releases** on version tags, application JAR attached —
  see [Releases](../../releases)

## Quick start

```bash
mvn clean install
mvn spring-boot:run
```

| Service | URL |
|---|---|
| Camunda Cockpit | http://localhost:8082/camunda |
| GPS Tracker | http://localhost:8082/gps.html |

Demo users (seeded at startup): 
| User | Password | Role | Email |
|------|----------|------|-------|
| acataluffi | a | Student | andrea.cataluffi@studenti.unicam.it |
| lmozzoni | a | Tutor | luca.mozzoni@unicam.it |
| a | a | Admin | - |

## Demo walkthrough

1. **Start a process** — log into the Cockpit, deploy a diagram from
   the Camunda Modeler, start an instance with a business key.
2. **Connect the tracker** — open the GPS Tracker, enter the user ID
   (must match `participantId`) and the business key, click Connect.
3. **Send coordinates** — set them manually, or click *Start Tracking*
   for automatic polling every 5 s; the *Randomize* buttons simulate
   movement. The running process reacts to geofence events in real time.

## Status

Research prototype — GPS input is simulated via the tracker page.
Not production-hardened.
