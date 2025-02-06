package wifiautoswitch

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.king.view.arcseekbar.ArcSeekBar
import com.wifiautoswitch.databinding.FragmentMainBinding
import kotlin.math.roundToInt

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("rssi_threshold", Context.MODE_PRIVATE)
        val rssiThresholdPresent = sharedPref.getFloat("last_rssi_thresholdPresent", 40F).toInt()
        binding.arcSeekBar.progress = rssiThresholdPresent
        binding.arcSeekBar.labelText = "${100 - rssiThresholdPresent}%"

        val rssi = "当前阈值为: ${((-90 + ((100 - rssiThresholdPresent) / 100f) * 90) * 10).roundToInt() / 10f} dBm"
        binding.textRssi.text = rssi

        binding.arcSeekBar.setOnChangeListener(object : ArcSeekBar.OnChangeListener {
            val handler = Handler(Looper.getMainLooper())

            override fun onStartTrackingTouch(isCanDrag: Boolean) {
                handler.removeCallbacksAndMessages(null)
            }

            override fun onProgressChanged(progress: Float, max: Float, fromUser: Boolean) {
                handler.removeCallbacksAndMessages(null)

                val currentInverseProgress = max - progress // 计算反向进度
                val currentRssiThreshold = ((-90 + (currentInverseProgress / 100f) * 60) * 10).roundToInt() / 10f

                binding.arcSeekBar.labelText = "${currentInverseProgress.toInt()}%"
                val text = "当前阈值为: $currentRssiThreshold dBm"
                binding.textRssi.text = text

                // 延迟记录最终进度
                handler.postDelayed({
                    Log.d("ArcSeekBar", "dBm 实际阈值: $currentRssiThreshold dBm")
                    Log.d("ArcSeekBar", "dBm 百分比阈值: $progress")

                    sharedPref.edit().putFloat("last_rssi_threshold", currentRssiThreshold).apply()
                    sharedPref.edit().putFloat("last_rssi_thresholdPresent", progress).apply()
                }, 500)
            }

            override fun onStopTrackingTouch(isCanDrag: Boolean) {
            }

            override fun onSingleTapUp() {
            }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}