package id.privy.livenessfirebasesdk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import id.privy.livenessfirebasesdk.common.Constant;
import id.privy.livenessfirebasesdk.entity.LivenessItem;
import id.privy.livenessfirebasesdk.listener.PrivyCameraLivenessCallBackListener;

public class LivenessApp {

    private static PrivyCameraLivenessCallBackListener callback;

    private Context context;

    private Bundle bundle;

    private LivenessApp(Context context, boolean isDebug, String successText, String instructions) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(Constant.Keys.IS_DEBUG, isDebug);
        bundle.putString(Constant.Keys.SUCCESS_TEXT, successText);
        bundle.putString(Constant.Keys.INSTRUCTION_TEXT, instructions);
        this.context = context;
        this.bundle = bundle;
    }

    public void start(PrivyCameraLivenessCallBackListener callback) {
        this.callback = callback;
        Intent i = new Intent(context, SimpleLivenessActivity.class);
        i.putExtras(bundle);
        context.startActivity(i);
    }

    public static void setCameraResultData(Bitmap bitmap){
        if (callback != null) {
            if (bitmap != null) {
                LivenessItem livenessItem = new LivenessItem();
                livenessItem.setImageBitmap(bitmap);
                callback.success(livenessItem);
            }
            else {
                callback.failed(new Throwable("ImageBase Not Found"));
            }
        }
        else {
            callback.failed(new Throwable("Null Callback CameraLiveness"));
        }
    }

    public static class Builder {

        private Context context;

        private boolean isDebug;

        private String successText;

        private String instructions;

        public Builder(Context context) {
            this.context = context;
            this.isDebug = false;
            this.successText = context.getString(R.string.success_text);
            this.successText = context.getString(R.string.instructions);
        }

        public LivenessApp.Builder setDebugMode(boolean isDebug) {
            this.isDebug = isDebug;
            return this;
        }

        public LivenessApp.Builder setSuccessText(String successText) {
            this.successText = successText;
            return this;
        }

        public LivenessApp.Builder setInstructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public LivenessApp build() {
            return new LivenessApp(context, isDebug, successText, instructions);
        }

    }

}
