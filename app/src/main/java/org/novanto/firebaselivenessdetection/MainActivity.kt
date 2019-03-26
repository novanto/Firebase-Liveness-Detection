package org.novanto.firebaselivenessdetection

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import id.privy.livenessfirebasesdk.LivenessApp
import id.privy.livenessfirebasesdk.entity.LivenessItem
import id.privy.livenessfirebasesdk.listener.PrivyCameraLivenessCallBackListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val livenessApp = LivenessApp(this)

        buttonStart.setOnClickListener {
            livenessApp.privyCameraLiveness(object : PrivyCameraLivenessCallBackListener {

                override fun success(livenessItem: LivenessItem?) {
                    if (livenessItem != null) {
                        test_image.setImageBitmap(livenessItem.imageBitmap)
                    }
                }

                override fun failed(t: Throwable?) {

                }

            })
        }
    }


}
