package id.privy.livenessfirebasesdk

import android.Manifest
import android.arch.lifecycle.Observer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import id.privy.livenessfirebasesdk.common.*
import id.privy.livenessfirebasesdk.event.LivenessEventProvider
import id.privy.livenessfirebasesdk.vision.VisionDetectionProcessor
import kotlinx.android.synthetic.main.activity_liveness_detection.*
import java.io.IOException
import java.util.Random
import kotlin.collections.ArrayList


class LivenessDetectionActivity : AppCompatActivity() {

    private val TAG = javaClass.simpleName

    private lateinit var errorContainer: View

    internal var preview: CameraSourcePreview? = null

    internal var graphicOverlay: GraphicOverlay? = null

    private var cameraSource: CameraSource? = null

    private var numberOfChallengeNeeded: Int = 0

    private var blinkCount = 0

    private var headShakeCount = 0

    private var mouthOpenCount = 0

    private var visionDetectionProcessor: VisionDetectionProcessor? = null

    private var challengeIndex = 0

    private var challengeOrder: ArrayList<Int> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liveness_detection)

        bindViews()

        if (PermissionUtil.with(this).isCameraPermissionGranted) {
            randomizeChallenge()
            createCameraSource()
            startNextChallenge()
            resetCount()
        }
        else {
            PermissionUtil.requestPermission(this, 1, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }

        LivenessEventProvider.getEventLiveData().observe(this, Observer {
            it?.let {
                when {
                    it.getType() == LivenessEventProvider.LivenessEvent.Type.Blink -> {
                        challengeInstructions(LivenessEventProvider.LivenessEvent.Type.Blink)
                        onBlinkEvent()
                    }
                    it.getType() == LivenessEventProvider.LivenessEvent.Type.HeadShake -> {
                        challengeInstructions(LivenessEventProvider.LivenessEvent.Type.HeadShake)
                        onHeadShakeEvent()
                    }
                    it.getType() == LivenessEventProvider.LivenessEvent.Type.MouthOpen -> {
                        challengeInstructions(LivenessEventProvider.LivenessEvent.Type.MouthOpen)
                        onMouthOpenedEvent()
                    }
                    it.getType() == LivenessEventProvider.LivenessEvent.Type.NotMatch -> {
                        challengeInstructions(LivenessEventProvider.LivenessEvent.Type.NotMatch)
                        errorContainer.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        randomizeChallenge()
        createCameraSource()
        startNextChallenge()
        resetCount()
    }

    override fun onResume() {
        super.onResume()
        startCameraSource()
        setRandomNumber()
    }

    override fun onPause() {
        super.onPause()
        preview?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource!!.release()
    }

    private fun bindViews() {
        graphicOverlay = findViewById(R.id.face_overlay)
        preview = findViewById(R.id.camera_source_preview)
        errorContainer = findViewById(R.id.container_error)
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(this, graphicOverlay)
            cameraSource!!.setFacing(CameraSource.CAMERA_FACING_FRONT)
        }

        visionDetectionProcessor = VisionDetectionProcessor()

        cameraSource!!.setMachineLearningFrameProcessor(visionDetectionProcessor)
        visionDetectionProcessor!!.setChallengeOrder(challengeOrder)
    }

    private fun resetCount() {
        headShakeCount = 0
        blinkCount = 0
        mouthOpenCount = 0

        errorContainer.visibility = View.GONE
    }

    fun navigateBack(success: Boolean, bitmap: Bitmap?) {
        if (bitmap != null) {
            if (success) {
                LivenessApp.setCameraResultData(BitmapUtils.processBitmap(bitmap))
                finish()
            } else {
                LivenessApp.setCameraResultData(null)
                finish()
            }
        }
    }


    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null")
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null")
                }
                preview!!.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource!!.release()
                cameraSource = null
            }

        }
    }

    private fun randomizeChallenge() {
        var i = 2
        val order = java.util.ArrayList<Int>()
        while (i >= 0) {
            var b = true
            val r = Random()
            val random = r.nextInt(3)

            if (order.size == 0) {
                order.add(random)
                i--
            }
            else {
                for (e in order) {
                    if (e == random) {
                        b = false
                    }
                }
                if (b) {
                    order.add(random)
                    i--
                }
            }
        }
        this.challengeOrder = order
    }

    private fun setRandomNumber() {
        this.numberOfChallengeNeeded = Random().nextInt(3) + 2
    }

    private fun setFixedNumber(number: Int) {
        this.numberOfChallengeNeeded = number
    }

    private fun startBlinkChallenge() {
        setRandomNumber()
        challengeInstructions(LivenessEventProvider.LivenessEvent.Type.Blink)
    }

    private fun startHeadShakeChallenge() {
        setFixedNumber(2)
        challengeInstructions(LivenessEventProvider.LivenessEvent.Type.HeadShake)
    }

    private fun startOpenMouthChallenge() {
        setRandomNumber()
        challengeInstructions(LivenessEventProvider.LivenessEvent.Type.MouthOpen)
    }

    private fun startNextChallenge() {
        if (challengeIndex >= challengeOrder.size) {
            finishChallenge()
        }
        else {
            textview_challenge_counter.text = 0.toString()
            when {
                challengeOrder[challengeIndex] == 0 -> startBlinkChallenge()
                challengeOrder[challengeIndex] == 1 -> startHeadShakeChallenge()
                challengeOrder[challengeIndex] == 2 -> startOpenMouthChallenge()
            }
        }
    }

    private fun onBlinkEvent() {
        blinkCount++
        textview_challenge_counter.text = blinkCount.toString()
        if (blinkCount == numberOfChallengeNeeded) {
            Toast.makeText(this, "Blink test succeed!", Toast.LENGTH_SHORT).show()
            challengeIndex++
            if (challengeIndex < challengeOrder.size) {
                visionDetectionProcessor!!.setVerificationStep(challengeOrder[challengeIndex])
            }
            startNextChallenge()
        }
    }

    private fun onHeadShakeEvent() {
        headShakeCount++
        textview_challenge_counter.text = headShakeCount.toString()

        if (headShakeCount == numberOfChallengeNeeded) {
            Toast.makeText(this, "HeadShake test succeed!", Toast.LENGTH_SHORT).show()
            challengeIndex++
            if (challengeIndex < challengeOrder.size) {
                visionDetectionProcessor!!.setVerificationStep(challengeOrder.get(challengeIndex))
            }
            startNextChallenge()
        }
    }

    private fun onMouthOpenedEvent() {
        mouthOpenCount++
        textview_challenge_counter.text = mouthOpenCount.toString()

        if (mouthOpenCount == numberOfChallengeNeeded) {
            Toast.makeText(this, "Open mouth test succeed!", Toast.LENGTH_SHORT).show()
            challengeIndex++
            if (challengeIndex < challengeOrder.size) {
                visionDetectionProcessor!!.setVerificationStep(challengeOrder.get(challengeIndex))
            }
            startNextChallenge()
        }
    }

    private fun finishChallenge() {
        this.setResult(RESULT_OK)
        errorContainer.visibility = View.VISIBLE
        container_challenge.visibility = View.GONE
        textview_challenge_warning.text = "Liveness Detection Success! \n Please look at the camera"
        Handler().postDelayed({
            cameraSource!!.takePicture(null, com.google.android.gms.vision.CameraSource.PictureCallback {
                navigateBack(true, BitmapFactory.decodeByteArray(it, 0, it.size))
            }) }, 1000
        )

    }

    private fun challengeInstructions(instructions: LivenessEventProvider.LivenessEvent.Type) = when (instructions) {
        LivenessEventProvider.LivenessEvent.Type.Blink -> {
            textview_challenge_name.text = "Blink needed"
            textview_challenge_label.text = "Blink count"
            textview_challenge_needed.text = numberOfChallengeNeeded.toString()
        }

        LivenessEventProvider.LivenessEvent.Type.HeadShake -> {
            textview_challenge_name.text = "Headshake needed"
            textview_challenge_label.text = "Headshake count"
            textview_challenge_needed.text = numberOfChallengeNeeded.toString()
        }

        LivenessEventProvider.LivenessEvent.Type.MouthOpen -> {
            textview_challenge_name.text = "Mouth Open needed"
            textview_challenge_label.text = "Mouth Open count"
            textview_challenge_needed.text = numberOfChallengeNeeded.toString()
        }

        else -> {

        }
    }
}
