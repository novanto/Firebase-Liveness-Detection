package id.privy.livenessfirebasesdk

import android.Manifest
import android.arch.lifecycle.Observer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import id.privy.livenessfirebasesdk.common.*
import id.privy.livenessfirebasesdk.event.LivenessEventProvider
import id.privy.livenessfirebasesdk.vision.VisionDetectionProcessor
import id.privy.livenessfirebasesdk.vision.VisionDetectionProcessor.Motion
import kotlinx.android.synthetic.main.activity_simple_liveness.*
import java.io.IOException
import java.util.*

class SimpleLivenessActivity : AppCompatActivity() {

    private val TAG = javaClass.simpleName

    internal var preview: CameraSourcePreview? = null

    internal var graphicOverlay: GraphicOverlay? = null

    private var cameraSource: CameraSource? = null

    private var visionDetectionProcessor: VisionDetectionProcessor? = null

    private var success = false

    private lateinit var successText: String

    private var isDebug = false

    private lateinit var motionInstructions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_liveness)

        preview = findViewById(R.id.cameraPreview)
        graphicOverlay = findViewById(R.id.faceOverlay)

        if (intent.extras != null) {
            val b = intent.extras!!
            successText = b.getString(Constant.Keys.SUCCESS_TEXT, getString(R.string.success_text))
            isDebug = b.getBoolean(Constant.Keys.IS_DEBUG, false)
            instructions.text = b.getString(Constant.Keys.INSTRUCTION_TEXT, getString(R.string.instructions))
            motionInstructions = b.getStringArray(Constant.Keys.MOTION_INSTRUCTIONS)
        }

        if (PermissionUtil.with(this).isCameraPermissionGranted) {
            createCameraSource()
            startHeadShakeChallenge()
        }
        else {
            PermissionUtil.requestPermission(this, 1, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }

        LivenessEventProvider.getEventLiveData().observe(this, Observer {
            it?.let {
                when {
                    it.getType() == LivenessEventProvider.LivenessEvent.Type.HeadShake -> {
                        onHeadShakeEvent()
                    }

                    it.getType() == LivenessEventProvider.LivenessEvent.Type.Default -> {
                        onDefaultEvent()
                    }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        createCameraSource()
    }

    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        preview?.stop()
        LivenessEventProvider.getEventLiveData().postValue(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource!!.release()
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(this, graphicOverlay)
            cameraSource!!.setFacing(CameraSource.CAMERA_FACING_FRONT)
        }

        val motion = Motion.values()[Random().nextInt(Motion.values().size)]

        when (motion) {
            Motion.Left -> {
                motionInstruction.text = this.motionInstructions[0]
            }

            Motion.Right -> {
                motionInstruction.text = this.motionInstructions[1]
            }
//
//            Motion.Up -> {
//                Toast.makeText(this, "Look Up", Toast.LENGTH_SHORT).show()
//            }
//
//            Motion.Down -> {
//                Toast.makeText(this, "Look Down", Toast.LENGTH_SHORT).show()
//            }
        }

        visionDetectionProcessor = VisionDetectionProcessor()
        visionDetectionProcessor!!.isSimpleLiveness(true, this, motion)
        visionDetectionProcessor!!.isDebugMode(isDebug)

        cameraSource!!.setMachineLearningFrameProcessor(visionDetectionProcessor)
    }

    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null")
                }
                preview!!.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource!!.release()
                cameraSource = null
            }
        }
    }

    fun navigateBack(success: Boolean, bitmap: Bitmap?) {
        if (bitmap != null) {
            if (success) {
                LivenessApp.setCameraResultData(BitmapUtils.processBitmap(bitmap))
                finish()
            }
            else {
                LivenessApp.setCameraResultData(null)
                finish()
            }
        }
    }

    private fun startHeadShakeChallenge() {
        visionDetectionProcessor!!.setVerificationStep(1)
    }

    private fun onHeadShakeEvent() {
        if (!success) {
            success = true
            motionInstruction.text = successText

            visionDetectionProcessor!!.setChallengeDone(true)
        }
    }

    private fun onDefaultEvent() {
        if (success) {
            Handler().postDelayed({
                cameraSource!!.takePicture(null, com.google.android.gms.vision.CameraSource.PictureCallback {
                    navigateBack(true, BitmapFactory.decodeByteArray(it, 0, it.size))
                })
            }, 500)
        }
    }

}
