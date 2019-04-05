package id.privy.livenessfirebasesdk.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import id.privy.livenessfirebasesdk.common.CameraImageGraphic
import id.privy.livenessfirebasesdk.common.DetectionThreshold
import id.privy.livenessfirebasesdk.common.FrameMetadata
import id.privy.livenessfirebasesdk.common.GraphicOverlay
import id.privy.livenessfirebasesdk.event.LivenessEventProvider
import id.privy.livenessfirebasesdk.event.LivenessEventProvider.LivenessEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import android.util.SparseArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.anko.doAsync


class VisionDetectionProcessor : VisionProcessorBase<List<FirebaseVisionFace>>() {

    private val detector: FirebaseVisionFaceDetector

    private var headState = 0

    private var state = 0

    private var isMouthOpen = false

    private var verificationStep: Int = 0

    private var faceId: Int = -1

    private var isEventSent: Boolean = false

    private var challengeOrder: ArrayList<Int> = ArrayList()

    private var isSimpleLiveness = false

    private lateinit var googleFaceDetector: FaceDetector

    enum class Motion {
        Left,
        Right,
        Up,
        Down
    }

    init {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setMinFaceSize(0.30f)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                .enableTracking()
                .build()


        detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
    }

    fun isSimpleLiveness(isSimpleLiveness: Boolean, context: Context) {
        this.isSimpleLiveness = isSimpleLiveness
        googleFaceDetector = FaceDetector.Builder(context)
                                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                                        .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                                        .setMode(FaceDetector.ACCURATE_MODE)
                                        .setTrackingEnabled(true)
                                        .build()
    }

    fun setVerificationStep(verificationStep: Int) {
        this.verificationStep = verificationStep
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: $e")
        }

    }

    override fun detectInImage(image: FirebaseVisionImage): Task<List<FirebaseVisionFace>> {
        return detector.detectInImage(image)
    }

    override fun onSuccess(
            originalCameraImage: Bitmap?,
            faces: List<FirebaseVisionFace>,
            frameMetadata: FrameMetadata,
            graphicOverlay: GraphicOverlay) {


        if (!isSimpleLiveness) {
            GlobalScope.launch {
                for (i in faces.indices) {
                    val face = faces[i]

                    val cameraFacing = frameMetadata?.cameraFacing
                    val faceGraphic = FaceGraphic(graphicOverlay, face, cameraFacing)
                    graphicOverlay.add(faceGraphic)


                    when (verificationStep) {
                        0 -> processBlink(face)
                        1 -> processHeadShake(face)
                        2 -> processOpenMouth(face)
                    }
                }
            }
        }
        else {
            runBlocking {
                GlobalScope.launch {
                    graphicOverlay.clear()
                    if (originalCameraImage != null) {
                        val imageGraphic = CameraImageGraphic(graphicOverlay, originalCameraImage)
                        graphicOverlay.add(imageGraphic)
                    }

                    val frame = Frame.Builder()
                        .setBitmap(originalCameraImage)
                        .build()
                    val faces = googleFaceDetector.detect(frame)

                    if (faces != null && faces.size() > 0) {
                        val face = getFaceFromArray(faces)
                        processHeadFacing(face, Motion.Left)
                    }
                }
                delay(100L)
            }
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Face detection failed $e")
    }

    /**
     *
     * @param face
     *
     * state 0 for both eyes opened
     * state 1 for one eye closed
     * state 2 for both eyes closed
     */
    private fun processBlink(face: FirebaseVisionFace) {
        val left = face.leftEyeOpenProbability
        val right = face.rightEyeOpenProbability
        if (left == FirebaseVisionFace.UNCOMPUTED_PROBABILITY || right == FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
            // At least one of the eyes was not detected.
            return
        }

        if (this.faceId == -1 || face.getTrackingId() == faceId) {
            when (state) {
                0 -> if (left > DetectionThreshold.EYE_OPEN_THRESHOLD && right > DetectionThreshold.EYE_OPEN_THRESHOLD) {
                    // Both eyes are initially open
                    state = 1
                }

                1 -> if (left < DetectionThreshold.EYE_CLOSED_THRESHOLD && right < DetectionThreshold.EYE_CLOSED_THRESHOLD) {
                    // Both eyes become closed
                    state = 2
                }

                2 -> if (left > DetectionThreshold.EYE_OPEN_THRESHOLD && right > DetectionThreshold.EYE_OPEN_THRESHOLD) {
                    // Both eyes are open again
                    Log.i("BlinkTracker", "blink occurred!")
                    val event = LivenessEvent()
                    if (faceId == -1) {
                        this.faceId = face.trackingId
                    }
                    event.setType(LivenessEvent.Type.Blink)
                    LivenessEventProvider.post(event)
                    state = 0
                }
            }
        }
        else {
            if (!isEventSent) {
                val event = LivenessEvent()
                event.setType(LivenessEvent.Type.NotMatch)
                LivenessEventProvider.post(event)
                isEventSent = true
            }
        }
    }


    /**
     *
     * @param face
     *
     * headState 0 for neutral
     * headState 1 for from left to right
     * headState 2 for from right to left
     */
    private fun processHeadShake(face: FirebaseVisionFace) {
        val headEulerAngleY = face.headEulerAngleY
        val event = LivenessEvent()
        event.setType(LivenessEvent.Type.HeadShake)

        if (this.faceId == -1 || face.getTrackingId() == faceId) {
            if (headEulerAngleY <= DetectionThreshold.LEFT_HEAD_THRESHOLD) {
                if (headState != 1) {
                    headState = 1
                    LivenessEventProvider.post(event)
                }
            } else if (headEulerAngleY >= DetectionThreshold.RIGHT_HEAD_THRESHOLD) {
                if (headState != 2) {
                    headState = 2
                    LivenessEventProvider.post(event)
                }
            }
        }
        else {
            if (!isEventSent) {
                event.setType(LivenessEvent.Type.NotMatch)
                LivenessEventProvider.post(event)
                isEventSent = true
            }
        }
    }

    private fun processHeadFacing(face: Face, motion: Motion) {
        val headEulerAngleY = face.eulerY
        val event = LivenessEvent()
        event.setType(LivenessEvent.Type.HeadShake)
        if (this.faceId == -1) {
            if (headEulerAngleY <= DetectionThreshold.LEFT_HEAD_FACING_THRESHOLD && motion == Motion.Left) {
                LivenessEventProvider.post(event)
            }
            else if (headEulerAngleY >= DetectionThreshold.RIGHT_HEAD_FACING_THRESHOLD  && motion == Motion.Right) {
                LivenessEventProvider.post(event)
            }
        }
        else {
            if (!isEventSent) {
                event.setType(LivenessEvent.Type.NotMatch)
                LivenessEventProvider.post(event)
                isEventSent = true
            }
        }
    }

    private fun getFaceFromArray(faceSparseArray: SparseArray<Face>): Face {
        var i = 0
        var isFace = false
        while (!isFace) {
            if (faceSparseArray.get(i) != null) {
                isFace = true
            } else {
                i++
            }
        }

        return faceSparseArray.get(i)
    }

    private fun processOpenMouth(face: FirebaseVisionFace?) {
        // https://stackoverflow.com/questions/42107466/android-mobile-vision-api-detect-mouth-is-open/43116414
        if (face == null) {
            return
        }

        if (this.faceId == -1 || face.getTrackingId() == faceId) {
            if (face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM) != null
                    && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT) != null
                    && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT) != null) {
                val cBottomMouthY = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM)!!.position.y!!
                val cLeftMouthY = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT)!!.position.y!!
                val cRightMouthY = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT)!!.position.y!!
                val centerPointY = (cLeftMouthY + cRightMouthY) / 2 - 15

                val differenceY = centerPointY - cBottomMouthY

                Log.i(TAG, "draw: difference Y >> $differenceY")

                if (differenceY < DetectionThreshold.OPEN_MOUTH_THRESHOLD && !isMouthOpen) {
                    val event = LivenessEvent()
                    event.setType(LivenessEvent.Type.MouthOpen)

                    isMouthOpen = true
                    LivenessEventProvider.post(event)
                }

                if (differenceY >= DetectionThreshold.CLOSED_MOUTH_THRESHOLD) {
                    isMouthOpen = false
                }
            }
        }
        else {
            if (!isEventSent) {
                val event = LivenessEvent()
                event.setType(LivenessEvent.Type.NotMatch)
                LivenessEventProvider.post(event)
                isEventSent = true
            }
        }
    }

    fun setChallengeOrder(challengeOrder: ArrayList<Int>) {
        this.challengeOrder = challengeOrder
        this.verificationStep = challengeOrder.get(0)
    }

    companion object {

        private val TAG = "VisionDetection"
    }

}
