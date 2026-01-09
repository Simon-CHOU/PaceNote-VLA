📄 SPEC.md (v2.0): PaceNote VLA - Pure Edge Edition
1. 核心愿景
构建一个完全运行在端侧的 AI 领航员，利用 Android 原生 AICore (或高通 AI Stack) 驱动的 SLM (Small Language Model)，实现“眼、手、口”同步的实时反馈，彻底消除云端延迟和底层对齐冲突。

2. 技术架构 (New Best Practice)
2.1 视觉引擎：从 TFLite 升级到原生感知
弃用：MediaPipe Tasks (因 16 KB 兼容性问题)。

采用：Android AICore / Google Gemini Nano (On-device) 或 Qualcomm AI Stack。

优势：系统级原生适配 Android 16，自动处理 16 KB 分页，直接调用 NPU 算力。

2.2 大脑：端侧 SLM (Small Language Model)
模型：Phi-3-mini 或 Gemini Nano (通过 AICore 接口)。

VLA 逻辑：

Vision Input：使用物理传感器触发的“截图采样”直接输入端侧多模态接口。

Action Output：模型输出不再仅仅是文字，而是直接生成 Action JSON（如：{"alert": "high", "tts": "Right mirror clear"}）。

2.3 语音与通信
TTS/ASR：使用 Android 系统自带的实时语音识别与合成，零网络开销。

本地总线：利用 Kotlin Flow 代替 WebRTC DataChannel，实现传感器与模型的内部通讯。

3. 重塑后的功能模块
模块,旧路径 (Cloud-based),新路径 (Pure Edge)
视觉,MediaPipe (SO 对齐冲突),CameraX + AICore 视觉流
决策,云端 LLM (网络延迟/鉴权),端侧 SLM (毫秒级响应)
通讯,LiveKit WebRTC (复杂/易崩),内部事件总线 (极其稳定)
部署,需要 APK 对齐 + 服务器,标准 Android 16 App 兼容

4. 为什么“推倒重来”更容易接近目标？
环境适配性：新架构直接使用 2026 年 Android 16 最推荐的 AICore 接口，从根源上解决了 16 KB 报错问题。

离线性能：拉力赛往往在山区，没有信号。全端侧方案是该业务唯一的真实出路。

开发效率：你不再需要处理 Token、API Secret、WebRTC 丢包和服务器运维，只需专注于 Android Kotlin + AICore。
