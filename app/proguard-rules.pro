# Host contract classes cross the Binder boundary by name (AIDL descriptors, PluginInfo Parcelable).
# The AARs ship consumer rules as well; this keeps the plugin safe if a staged AAR omits them.
# zh-CN: 宿主契约类按名称跨越 Binder 边界 (AIDL 描述符, PluginInfo Parcelable). AAR 自带 consumer 规则, 此处在 AAR 缺少规则时兜底.
-keep class org.autojs.plugin.** { *; }

# sherpa-onnx (roadmap D13, P0.2): the AAR ships no consumer rules, while libsherpa-onnx-jni.so binds its native methods
# by name and reads every config object field by name (GetFieldID) and builds result objects such as SpeechSegment
# through their constructors. Renaming or removing any member breaks the JNI side at run time, so the whole Kotlin API
# is kept as is.
# zh-CN: sherpa-onnx (路线图 D13, P0.2): AAR 不带 consumer 规则, 而 libsherpa-onnx-jni.so 按名称绑定原生方法, 按名称读取每个
# 配置对象的字段 (GetFieldID), 并通过构造函数创建 SpeechSegment 等结果对象. 任何成员被改名或移除都会在运行时破坏 JNI 一侧,
# 因此完整保留其 Kotlin API.
-keep class com.k2fsa.sherpa.onnx.** { *; }

# The TTS generation callback (roadmap P4.1): the JNI side calls invoke([F)Ljava/lang/Integer; of the object it is given.
# zh-CN: TTS 生成回调 (路线图 P4.1): JNI 一侧按名称调用所给对象的 invoke([F)Ljava/lang/Integer;.
-keep class io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsEngine$SampleCallback { *; }

# The diarization progress callback (roadmap P8.4.3): the JNI side calls invoke(IIJ)Ljava/lang/Integer; of the object it is given.
# zh-CN: 说话人分离的进度回调 (路线图 P8.4.3): JNI 一侧按名称调用所给对象的 invoke(IIJ)Ljava/lang/Integer;.
-keep class io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.DiarizationEngine$Progress { *; }

# PluginInfo in common-plugin-api carries the compile-time @Parcelize annotation, whose class is not on the runtime classpath.
# zh-CN: common-plugin-api 中的 PluginInfo 带有编译期注解 @Parcelize, 其类不在运行时类路径上.
-dontwarn kotlinx.parcelize.Parcelize
