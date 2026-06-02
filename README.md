# CGDiffViewer

当我们在观看CG差分时，很多差分一大堆图片都是一样的，那么我一页一页翻就很不爽，但是制作成GIF又不方便不好用，所以就有了这个东西。本质自用，之后想到的能使用的功能还会加。

<img width="450"  alt="664d5b8389ac5d9bb6b8e0f460f3ef26" src="https://github.com/user-attachments/assets/11cc92a9-6f5b-42da-9730-f626423d275c" />
<img width="450" alt="8f32fd3505ef74156312fd8670a73982" src="https://github.com/user-attachments/assets/c88bf1b0-b19f-41d9-9e7f-e66ffff467ff" />
<img width="479" height="378" alt="图标" src="https://github.com/user-attachments/assets/a148fbef-34f1-489f-8a31-9fd97c399cfd" />
下面都是ai写的...

# 图片序列播放器

一个Android应用，用于播放图片序列，支持多种播放控制功能。

## 功能特性

1. **图片序列播放**：按文件名顺序播放图片序列
2. **多种格式支持**：支持PNG、JPG、WebP等常见图片格式
3. **精确速度控制**：播放速度精确到小数点后一位（如20.5张/秒）
4. **全屏横屏播放**：支持全屏横屏模式
5. **长按加速**：长按屏幕可加快播放速度
6. **播放历史**：保存最近50个播放记录
7. **断点续播**：记住上次播放位置
8. **视频风格控制**：点击屏幕弹出控制面板，自动隐藏

## 项目结构

```
app/src/main/java/com/example/imageviewer/
├── MainActivity.kt          # 主Activity
├── MainScreen.kt            # 主界面（历史网格）
├── PlayerScreen.kt          # 播放界面
├── SettingsScreen.kt        # 设置界面
├── MainViewModel.kt         # 数据管理
├── data/                    # 数据层
│   ├── ImageSequence.kt     # 数据实体
│   ├── HistoryDao.kt        # 数据库操作
│   ├── AppDatabase.kt       # 数据库配置
│   └── SettingsDataStore.kt # 设置存储
├── ui/components/           # UI组件
│   ├── PlayerControls.kt    # 播放控制面板
│   └── SpeedControl.kt      # 速度控制组件
├── ui/theme/                # 主题配置
└── util/                    # 工具类
    ├── FileUtils.kt         # 文件操作工具
    └── SpeedUtils.kt        # 速度转换工具
```

## 构建APK

### 前提条件

1. 安装 [Android Studio](https://developer.android.com/studio)
2. 安装 Android SDK (API 34)
3. 配置Java开发环境 (JDK 8+)

### 构建步骤

1. **打开项目**：
   - 启动Android Studio
   - 选择"Open an existing Android Studio project"
   - 选择项目根目录

2. **同步项目**：
   - Android Studio会自动同步Gradle
   - 等待依赖下载完成

3. **构建APK**：
   - 菜单栏：Build → Generate Signed Bundle/APK
   - 选择"APK" → Next
   - 创建或选择密钥库（用于签名）
   - 选择"release"构建类型
   - 点击"Finish"

4. **获取APK**：
   - 构建完成后，APK位于 `app/build/outputs/apk/release/`
   - 文件名：`app-release.apk`

### 调试版本

1. 连接Android设备或启动模拟器
2. 点击运行按钮（绿色三角形）
3. 应用将安装到设备上

## 使用说明

### 添加图片序列

1. 点击右下角"+"按钮
2. 选择"选择文件夹"或"选择文件"
3. 选择包含图片的文件夹或多个图片文件

### 播放控制

1. **播放/暂停**：点击播放/暂停按钮
2. **调节速度**：点击速度显示区域，拖动滑块
3. **全屏**：点击全屏按钮
4. **长按加速**：长按屏幕2倍速播放
5. **进度控制**：拖动进度条跳转

### 设置

1. 点击右上角设置图标
2. 可调节：
   - 默认播放速度
   - 长按加速倍数
   - 控制面板自动隐藏时间

## 技术栈

- Kotlin
- Jetpack Compose
- Room数据库
- Coil图片加载
- Navigation组件
- DataStore存储

## 依赖项

- AndroidX Core KTX
- Jetpack Compose BOM
- Material3
- Room数据库
- Coil图片加载
- Navigation Compose
- DataStore Preferences
- Gson JSON处理

## 权限要求

- `READ_EXTERNAL_STORAGE`：读取外部存储
- `READ_MEDIA_IMAGES`：读取图片媒体文件
- `WRITE_EXTERNAL_STORAGE`：写入外部存储（仅Android 9及以下）

## 注意事项

1. **Android版本**：支持Android 8.0 (API 26)及以上
2. **存储权限**：首次运行需要授予存储权限
3. **文件格式**：支持JPG、PNG、WebP、GIF、BMP、TIFF
4. **性能建议**：大量图片时建议分批次加载

## 故障排除

### 应用无法读取图片

1. 检查存储权限是否授予
2. 确认图片文件路径正确
3. 尝试重新选择文件夹

### 播放卡顿

1. 降低播放速度
2. 减少同时加载的图片数量
3. 检查设备性能

### 构建失败

1. 检查Android Studio版本
2. 更新Gradle插件
3. 清理项目：Build → Clean Project

## 开发说明

### 添加新功能

1. 在相应的ViewModel中添加业务逻辑
2. 创建新的Composable函数作为UI组件
3. 更新Navigation路由

### 修改主题

1. 编辑 `ui/theme/Color.kt` 修改颜色
2. 编辑 `ui/theme/Type.kt` 修改字体
3. 编辑 `ui/theme/Theme.kt` 修改主题配置

## 许可证

本项目仅供学习和个人使用。
