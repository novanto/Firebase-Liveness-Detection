package id.privy.livenessfirebasesdk;

import android.content.Context;
import android.content.Intent;
import id.privy.livenessfirebasesdk.entity.LivenessItem;
import id.privy.livenessfirebasesdk.listener.PrivyCameraLivenessCallBackListener;

public class LivenessApp {

    private static PrivyCameraLivenessCallBackListener callback;

    private Context context;

    public LivenessApp(Context context) {
        this.context = context;
    }

    public void privyCameraLiveness(PrivyCameraLivenessCallBackListener callback) {
        this.callback = callback;
        Intent i = new Intent(context, LivenessDetectionActivity.class);
        context.startActivity(i);
    }

    public static void setDataCameraSelfi(String baseImage1,String baseImage2){
        if (callback != null) {
            if(baseImage1!=null && baseImage2 != null){
                LivenessItem livenessItem = new LivenessItem();
                livenessItem.setBaseImage1(baseImage1);
                livenessItem.setBaseImage2(baseImage2);
                callback.success(livenessItem);
            }else{
                callback.failed(new Throwable("ImageBase Not Found"));
            }
        }else{
            callback.failed(new Throwable("Null Callback CameraLiveness"));
        }
    }
}
