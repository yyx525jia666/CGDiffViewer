# 构建APK详细步骤

## 前提条件

1. **安装Android Studio**
   - 下载地址：https://developer.android.com/studio
   - 完成安装并启动

2. **安装Android SDK**
   - 在Android Studio中，打开SDK Manager
   - 安装Android 13 (API 34) SDK
   - 安装Android SDK Build-Tools 34.0.0

3. **配置环境变量**
   - 设置JAVA_HOME指向JDK 11或17
   - 将Android SDK的platform-tools添加到PATH

## 构建步骤

### 方法一：使用Android Studio（推荐）

1. **打开项目**
   ```
   File → Open → 选择项目根目录
   ```

2. **同步Gradle**
   - Android Studio会自动同步
   - 等待依赖下载完成（首次可能需要几分钟）

3. **构建Debug APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **构建Release APK**
   ```
   Build → Generate Signed Bundle / APK
   → 选择APK → Next
   → 创建或选择密钥库
   → 选择release构建类型
   → Finish
   ```

5. **获取APK**
   - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
   - Release APK: `app/build/outputs/apk/release/app-release.apk`

### 方法二：使用命令行

1. **打开终端**
   - 在项目根目录打开命令提示符

2. **构建Debug APK**
   ```bash
   gradlew.bat assembleDebug
   ```

3. **构建Release APK**
   ```bash
   gradlew.bat assembleRelease
   ```

4. **查找APK文件**
   ```bash
   dir app\build\outputs\apk\
   ```

## 签名配置

### 创建密钥库

1. 在Android Studio中：
   ```
   Build → Generate Signed Bundle / APK
   → Create new...
   → 填写密钥库信息
   → OK
   ```

2. 或使用命令行：
   ```bash
   keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
   ```

### 配置build.gradle

在`app/build.gradle.kts`中添加签名配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.jks")
            storePassword = "your_store_password"
            keyAlias = "my-alias"
            keyPassword = "your_key_password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## 常见问题

### 1. Gradle同步失败
- 检查网络连接
- 更新Gradle版本
- 清理缓存：`File → Invalidate Caches / Restart`

### 2. 构建错误
- 检查SDK版本是否正确
- 更新依赖版本
- 清理项目：`Build → Clean Project`

### 3. 签名错误
- 检查密钥库路径
- 确认密码正确
- 检查密钥别名

## 优化APK

### 启用ProGuard
在`app/build.gradle.kts`中：
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 启用R8
R8默认启用，比ProGuard更快更高效。

## 测试APK

### 安装到设备
```bash
adb install app-debug.apk
```

### 启动应用
```bash
adb shell am start -n com.example.imageviewer/.MainActivity
```

## 发布到应用商店

1. **生成签名APK**
2. **优化资源**
3. **测试兼容性**
4. **上传到Google Play Console**

## 项目结构说明

```
app/
├── src/main/
│   ├── java/com/example/imageviewer/  # Kotlin源代码
│   ├── res/                           # 资源文件
│   └── AndroidManifest.xml            # 应用配置
├── build.gradle.kts                   # 应用构建配置
└── proguard-rules.pro                 # 代码混淆规则

build.gradle.kts                       # 项目构建配置
settings.gradle.kts                    # 项目设置
gradle.properties                      # Gradle配置
```

## 依赖说明

```kotlin
// 核心依赖
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.activity:activity-compose:1.8.2")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// Room数据库
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// 图片加载
implementation("io.coil-kt:coil-compose:2.5.0")

// 导航
implementation("androidx.navigation:navigation-compose:2.7.7")

// 数据存储
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

## 技术支持

如有问题，请检查：
1. Android Studio版本
2. Gradle版本
3. SDK版本
4. 依赖版本兼容性