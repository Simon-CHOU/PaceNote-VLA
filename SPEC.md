# Project Spec: PaceNote VLA (Android)

## 1. Executive Summary
**Project Name:** PaceNote VLA
**Tagline:** Your Vision-Language-Action AI Co-driver.
**Concept:** A high-performance driving companion inspired by WRC Navigators. It utilizes the Snapdragon 8 Gen 3 NPU for local reflex sensing and Cloud VLM (GLM-4V/Qwen-VL) for cognitive reasoning.
**Core Tech:** Hybrid AI (Local MediaPipe + Cloud VLA), WebRTC (LiveKit), Sensor Fusion.
**Disclaimer:** Entertainment and companionship only. Not for safety-critical ADAS.

---

## 2. System Architecture: The "Fast & Slow" VLA Loops

### 2.1 The "Reflex" Loop (Local Edge - High Frequency)
* **Compute:** NPU/GPU (SD 8 Gen 3 Hexagon/Adreno).
* **Vision:** MediaPipe Object Detection (Targeting Right Mirror ROI).
* **Telemetry:** Accelerometer/Gyroscope (50Hz) logging G-Force and Yaw.
* **Logic:** - If Object in Mirror ROI -> Trigger Local TTS Alert.
    - If Lateral G > 0.3g -> Send `CRITICAL_MANEUVER` signal to Cloud Agent.

### 2.2 The "Cognitive" Loop (Cloud Real-time - Adaptive)
* **Protocol:** LiveKit (WebRTC) for full-duplex Audio/Video.
* **Logic:** - **Cruising Mode:** Low-frequency visual analysis (every 15s).
    - **Maneuver Mode:** High-frequency visual analysis (every 2-4s) triggered by Reflex Loop.
* **Persona:** WRC Navigator / Driving Coach (High verbosity, punchy callouts).

---

## 3. Key Feature Specifications

### 3.1 Adaptive VLA Sampling
- **Action-Triggered Sensing:** The App dynamically adjusts the sampling rate of video frames sent to the LLM based on real-time G-Force telemetry. 
- **Barge-in Support:** AI must immediately stop speaking if the Driver speaks or if a local "Reflex" alert is triggered.

### 3.2 UI & Visualization
- **Camera Preview:** With user-adjustable ROI overlays for "Right Mirror" and "Windshield".
- **Telemetry HUD:** A WRC-style G-Force Friction Circle and real-time GPS/Speedometer.
- **Log System:** Exports CSV logs including Time, Lat/Lon, G-Force, and AI Commentary.

### 3.3 Hardware Strategy & Fallback
- **Tier 1 (SD 8 Gen 3):** Max NPU utilization, 1080p WebRTC stream, high-fidelity HUD.
- **Tier 2 (Legacy):** Graceful degradation to CPU-based TFLite, 720p/480p stream, basic HUD.

### 3.4 Commercial & i18n
- **Auth:** Firebase (Google, GitHub, Apple, etc.).
- **Monetization:** 7-day trial -> Ad-supported Free Tier (AdMob) or Pro Subscription (Google Play Billing).
- **i18n:** LHD/RHD toggle; Support for English and Chinese.

---

## 4. Technical Stack
- **Android:** Kotlin, Jetpack Compose, CameraX, Hilt, MediaPipe.
- **Real-time:** LiveKit SDK (Android & Python Agent).
- **Backend:** Python-based LiveKit Agent connecting to GLM-4V/Qwen-VL APIs.