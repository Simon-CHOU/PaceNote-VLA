V1.0 失败复盘 (2026-01-09):

底层冲突：Android 16 强制 16KB 页对齐，旧版 MediaPipe/LiveKit 二进制库无法加载。

链路过长：WebRTC + 云端 VLM 在极端驾驶环境下可靠性存疑。

兼容性代价：降低 targetSdk 虽能编译，但无法在 Android 16 真机点火。