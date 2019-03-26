package id.privy.livenessfirebasesdk.entity;

import android.graphics.Bitmap;

public class LivenessItem {

    String baseImage1;

    String baseImage2;

    Bitmap imageBitmap;

    public String getBaseImage1() {
        return baseImage1;
    }

    public void setBaseImage1(String baseImage1) {
        this.baseImage1 = baseImage1;
    }

    public String getBaseImage2() {
        return baseImage2;
    }

    public void setBaseImage2(String baseImage2) {
        this.baseImage2 = baseImage2;
    }

    public Bitmap getImageBitmap() {
        return this.imageBitmap;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }
}
