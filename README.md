
# Firebase-Liveness-Detection  
Lightweight Liveness Detection using Firebase ML Kit - file size less than 5 Mb. Currently only available on detecting left and right head movement
  
  
# Implementation  
  
Add maven script
    
       allprojects {
           repositories {
               jcenter()
               maven {
                    url 'https://dl.bintray.com/novanto/LivenessDetection'
               }
           }
       }
  
Add your `google-services.json` file to the app  
  
Add this script to your app gradle file:  
    
    dependencies {
        implementation 'com.google.firebase:firebase-core:16.0.8'
        implementation 'com.google.firebase:firebase-ml-vision:19.0.3'
        implementation 'id.privy.livenessfirebasesdk:livenessDetection:0.0.6'
    }    
      
Add builder to your activity to start  

    LivenessApp livenessApp = LivenessApp.Builder(this)  
    			 .setDebugMode(false) //to enable face landmark detection 
    			 .setSuccessText($SUCCESSTEXT) 
    			 .setInstructions($INSTRUCTIONS) 
    			 .setMotionInstruction($LEFT_MOTION_INSTRUCTION", $RIGHT_MOTION_INSTRUCTION)
    			 .build()

Initiate with callback

     livenessapp.start(object : PrivyCameraLivenessCallBackListener {  
        override fun success(livenessItem: LivenessItem?) {  
		      //your callback here
        }  
      
        override fun failed(t: Throwable?) {  
		      //handle error here
        }  
    })

`LivenessItem` is an instance of model that return a bitmap with `getImageBitmap()` so that you can set the image directly

