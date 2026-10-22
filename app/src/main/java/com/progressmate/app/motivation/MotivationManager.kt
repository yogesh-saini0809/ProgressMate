package com.progressmate.app.motivation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.random.Random

enum class AppLanguage { ENGLISH, HINDI, HINGLISH }

object MotivationManager {
    private val englishMessages = listOf(
        "Great job! You are getting closer to your goal.",
        "Small progress is still progress. Keep going!",
        "Awesome! Step by step wins the race.",
        "You carved out time for yourself today. Well done!",
        "Session complete! Rest up, you earned it."
    )

    private val hindiMessages = listOf(
        "शाबाश! एक और कदम पूरा हुआ।",
        "बहुत बढ़िया! आप अपने लक्ष्य के करीब पहुँच रहे हैं।",
        "छोटी प्रगति भी प्रगति होती है। लगे रहो!",
        "शानदार! आज आपने अपने भविष्य के लिए समय निकाला।",
        "ब्रेक का समय! आपने इसे अपनी मेहनत से कमाया है।"
    )

    private val hinglishMessages = listOf(
        "Shabash! Ek aur task complete hua.",
        "Great job! Tum apne goal ke aur kareeb aa gaye ho.",
        "Chhoti progress bhi progress hoti hai. Rukna mat!",
        "Aaj tumne honestly focus kiya, proud of you!",
        "Break time! Chai/paani piyo, you earned it."
    )

    fun getRandomCheer(language: AppLanguage): String {
        val list = when (language) {
            AppLanguage.ENGLISH -> englishMessages
            AppLanguage.HINDI -> hindiMessages
            AppLanguage.HINGLISH -> hinglishMessages
        }
        return list[Random.nextInt(list.size)]
    }

    fun triggerHapticFeedback(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(70)
        }
    }
}
