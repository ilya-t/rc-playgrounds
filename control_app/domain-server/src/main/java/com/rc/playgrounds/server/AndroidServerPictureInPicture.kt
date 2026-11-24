package com.rc.playgrounds.server

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational

class AndroidServerPictureInPicture(
    private val activity: Activity,
) : ServerPictureInPicture {
    init {
        publishParams()
    }

    override fun enter() {
        if (!canEnter()) {
            return
        }
        val params = publishParams() ?: return
        activity.enterPictureInPictureMode(params)
    }

    private fun canEnter(): Boolean {
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
            !activity.isInPictureInPictureMode
    }

    private fun publishParams(): PictureInPictureParams? {
        if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            return null
        }
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio(bounds.width(), bounds.height()))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                }
            }
            .build()
        activity.setPictureInPictureParams(params)
        return params
    }

    private fun aspectRatio(width: Int, height: Int): Rational {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val ratio = safeWidth.toFloat() / safeHeight.toFloat()
        if (ratio > MIN_ASPECT && ratio < MAX_ASPECT) {
            return Rational(safeWidth, safeHeight)
        }
        return if (ratio >= MAX_ASPECT) {
            Rational((safeHeight * MAX_ASPECT_FIT).toInt().coerceAtLeast(1), safeHeight)
        } else {
            Rational(safeWidth, (safeWidth / MIN_ASPECT_FIT).toInt().coerceAtLeast(1))
        }
    }

    private companion object {
        const val MIN_ASPECT = 1f / 2.39f
        const val MAX_ASPECT = 2.39f
        const val MIN_ASPECT_FIT = 0.42f
        const val MAX_ASPECT_FIT = 2.38f
    }
}
