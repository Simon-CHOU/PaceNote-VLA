# MediaPipe Assets

This folder contains the ML models for object detection.

## Required Files

### object_detector.tflite
- **Source**: https://ai.google.dev/edge/mediapipe/solutions/vision/object_detector
- **Model**: EfficientDet Lite0
- **Training**: COCO dataset (80 classes)
- **Input**: 192x192 RGB images
- **Output**: Bounding boxes with class labels

### labels.txt
- C0CO class labels (one per line)
- Used to map model output indices to class names

## How to Add the Model

1. Download `efficientdet_lite0.tflite` from:
   https://ai.google.dev/edge/mediapipe/solutions/vision/object_detector

2. Rename it to `object_detector.tflite`

3. Place it in this assets folder:
   `feature/media-pipe/src/main/assets/object_detector.tflite`

## Class Labels

The model detects 80 COCO classes. Relevant classes for driving:

- **Vehicles**: car, truck, bus, motorcycle, bicycle, train, boat
- **People**: person
- **Traffic**: traffic light, stop sign, fire hydrant, parking meter

Full list in `labels.txt`

## Performance

- **NPU (Hexagon)**: ~30ms on SD 8 Gen 3
- **GPU (Adreno)**: ~40ms
- **CPU**: ~100ms

The app automatically falls back to CPU if NPU/GPU fails.
