package com.example.imageviewer.util

object SpeedUtils {
    
    /**
     * 将"张/秒"转换为"毫秒/张"
     * @param imagesPerSecond 每秒播放的图片数量
     * @return 每张图片显示的毫秒数
     */
    fun toDelayMillis(imagesPerSecond: Double): Long {
        return (1000.0 / imagesPerSecond).toLong()
    }
    
    /**
     * 将"毫秒/张"转换为"张/秒"
     * @param delayMillis 每张图片显示的毫秒数
     * @return 每秒播放的图片数量
     */
    fun toImagesPerSecond(delayMillis: Long): Double {
        return 1000.0 / delayMillis
    }
    
    /**
     * 格式化速度显示
     * @param speed 张/秒
     * @return 格式化后的字符串，如"20.5张/秒"
     */
    fun formatSpeed(speed: Double): String {
        return "%.1f张/秒".format(speed)
    }
    
    /**
     * 限制速度范围
     * @param speed 输入速度
     * @return 限制在0.1-60.0范围内的速度
     */
    fun clampSpeed(speed: Double): Double {
        return speed.coerceIn(0.1, 60.0)
    }
}
