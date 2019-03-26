package id.privy.livenessfirebasesdk.listener;

import id.privy.livenessfirebasesdk.entity.LivenessItem;

public interface PrivyCameraLivenessCallBackListener {

    void success(LivenessItem livenessItem);

    void failed(Throwable t);
}

