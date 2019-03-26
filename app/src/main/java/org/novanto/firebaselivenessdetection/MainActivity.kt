package org.novanto.firebaselivenessdetection

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import id.privy.livenessfirebasesdk.LivenessApp
import id.privy.livenessfirebasesdk.entity.LivenessItem
import id.privy.livenessfirebasesdk.listener.PrivyCameraLivenessCallBackListener

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val livenessApp = LivenessApp(this)

        livenessApp.privyCameraLiveness(object : PrivyCameraLivenessCallBackListener {

            override fun success(livenessItem: LivenessItem?) {

            }

            override fun failed(t: Throwable?) {

            }

        })
    }


}
