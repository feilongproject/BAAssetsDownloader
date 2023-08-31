package com.feilongproject.baassetsdownloader

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.view.drawToBitmap
import com.feilongproject.baassetsdownloader.databinding.WidgetConfigBinding
import com.feilongproject.baassetsdownloader.provider.WidgetProvider


class WidgetConfigure : Activity() {
    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID


    override fun onCreate(icicle: Bundle?) {
        Log.e("FLP_ERROR", "AppWidgetManager.INVALID_APPWIDGET_ID ${intent.data}")

        super.onCreate(icicle)
        setResult(RESULT_CANCELED)

        mAppWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        Log.d("FLP_DEBUG", "mAppWidgetId: $mAppWidgetId")

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            val value = intent.getStringExtra("key")
            Log.e("FLP_ERROR", "AppWidgetManager.INVALID_APPWIDGET_ID ${intent.data}")
            finish()
        }

        val binding = WidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 从设置中获取组件信息
        val pref = Pref(baseContext, "widgetConfig")
        binding.selectAmbiguity.progress = pref.getValue("ambiguity", binding.selectAmbiguity.progress).also {
            Log.d("FLP_DEBUG", "widgetConfig.ambiguity: $it")
            binding.ambiguityValue.text = String.format("%.2f", it / 100.0)
            binding.root.post {
                ambiguityPreview(baseContext, binding.preview, it)
            }
        }
        binding.selectTransparency.progress = pref.getValue("transparency", binding.selectTransparency.progress).also {
            Log.d("FLP_DEBUG", "widgetConfig.transparency: $it")
            binding.transparencyValue.text = String.format("%.2f", it / 100.0)
            binding.preview.alpha = (it / 100.0).toFloat()
        }
        binding.serverType.check(pref.getValue("serverType", selectServer(binding)).let {
            Log.d("FLP_DEBUG", "widgetConfig.serverType: $it")
            when (it) {
                "jpServer" -> binding.selectServerJp.id
                "globalServer" -> binding.selectServerGlobal.id
                "cnServer" -> binding.selectServerCn.id
                else -> binding.serverType.checkedRadioButtonId
            }
        })


        binding.enter.setOnClickListener {
            pref.putValue("ambiguity", binding.selectAmbiguity.progress)
            pref.putValue("transparency", binding.selectTransparency.progress)
            pref.putValue("serverType", selectServer(binding))
            setResult(RESULT_OK, Intent().also { it.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId) })
            WidgetProvider().updateWidget(baseContext, AppWidgetManager.getInstance(baseContext), mAppWidgetId)
            finish()
        }

        binding.quit.setOnClickListener {
            finish()
        }

        binding.serverType.setOnCheckedChangeListener { _, _ ->
            Log.d("FLP_DEBUG", "select server: ${selectServer(binding)}")
        }

        binding.selectAmbiguity.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.ambiguityValue.text = String.format("%.2f", progress / 100.0)
                ambiguityPreview(baseContext, binding.preview, seekBar.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Log.d("FLP_DEBUG", "开始滑动 ${seekBar.progress}")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.d("FLP_DEBUG", "停止滑动 ${seekBar.progress}")
            }
        })

        binding.selectTransparency.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.transparencyValue.text = String.format("%.2f", progress / 100.0)
                binding.preview.alpha = (progress / 100.0).toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Log.d("FLP_DEBUG", "开始滑动 ${seekBar.progress}")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.d("FLP_DEBUG", "停止滑动 ${seekBar.progress}")
            }
        })

    }

    private fun selectServer(binding: WidgetConfigBinding): String {
        return when (binding.serverType.checkedRadioButtonId) {
            binding.selectServerJp.id -> "jpServer"
            binding.selectServerGlobal.id -> "globalServer"
            binding.selectServerCn.id -> "cnServer"
            else -> throw Throwable("not found select server")
        }
    }

    private var srcBitmap: Bitmap? = null

    private fun ambiguityPreview(context: Context, view: ImageView, value: Int) {
        if (srcBitmap == null) srcBitmap = view.drawToBitmap()
        Log.d("FLP_DEBUG", "$value bitmap src: ${srcBitmap?.width}x${srcBitmap?.height}")

        val overlay = blur(context, srcBitmap!!, value / 4f)
        Log.d("FLP_DEBUG", "overlay: ${overlay.width}x${overlay.height}")
        view.setImageBitmap(overlay)
    }

    companion object {
        @Deprecated("Deprecated in Java")
        fun blur(context: Context, image: Bitmap, radius: Float): Bitmap {
            if (radius == 0f) return image
            Log.d("FLP_DEBUG", "blur: radius: $radius")
            val rs = RenderScript.create(context)
            val outputBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            val intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            val allIn = Allocation.createFromBitmap(rs, image)
            val allOut = Allocation.createFromBitmap(rs, outputBitmap)
            intrinsicBlur.setRadius(radius)
            intrinsicBlur.setInput(allIn)
            intrinsicBlur.forEach(allOut)
            allOut.copyTo(outputBitmap)
//        image.recycle()
            rs.destroy()
            Log.d("FLP_DEBUG", "blur: ${outputBitmap.height}x${outputBitmap.width}")
            return outputBitmap
        }
    }

}


