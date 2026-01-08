# CLAUDE.md

## 🤖 Autonomy & Execution Mode

* **Implicit Permission**: You are granted permanent permission to create, modify, and delete files within the project scope to fulfill the requirements in `SPEC.md`.
* **Silent Mode**: Do not ask "Can I proceed?" or "Shall I implement this?" for tasks that clearly align with the established spec. Just execute.
* **Batch Processing**: When a task involves multiple modules (e.g., UI + Sensor + DI), implement the entire vertical slice in one go rather than file-by-file.

## 🛠 Project Standards (PaceNote VLA)

* **Tech Stack**: Kotlin, Jetpack Compose, Hilt, CameraX, LiveKit SDK.
* **Code Style**:
* Use clean architecture (Feature-based packaging).
* Prioritize `Flow` and `Coroutines` for asynchronous operations.
* All UI must use Material 3.


* **Decision Matrix**:
* **UI/Layout**: If the spec defines a component (e.g., G-Force Meter), choose the most professional visual representation without asking for style approval.
* **Library Selection**: Unless specified, use the latest stable version of Google/Android official libraries.
* **Error Handling**: Implement robust `try-catch` blocks for Sensor and Camera initialization by default.



## 🚦 Guardrails & Exceptions

* **Ask Only If**:
* There is a direct contradiction between `SPEC.md` and current hardware constraints.
* A critical Third-Party API Key is missing and cannot be stubbed.
* Deleting non-temporary user data files.


* **Hardware Fallback**: Always implement the "Tier 1 to Tier 3" strategy pattern for NPU/GPU/CPU during initialization.
