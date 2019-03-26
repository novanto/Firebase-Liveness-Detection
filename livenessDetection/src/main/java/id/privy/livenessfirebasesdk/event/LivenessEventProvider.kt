package id.privy.livenessfirebasesdk.event

import android.arch.lifecycle.MutableLiveData

object LivenessEventProvider {

    private val liveDataEvent = MutableLiveData<LivenessEvent>()

    fun post(event:LivenessEvent) {
        this.liveDataEvent.postValue(event)
    }

    fun getEventLiveData(): MutableLiveData<LivenessEvent> {
        return liveDataEvent
    }

    class LivenessEvent {

        enum class Type {
            Blink,
            HeadShake,
            MouthOpen,
            NotMatch,
            Default
        }

        private var type = Type.Default

        fun setType(type :Type) {
            this.type = type
        }

        fun getType(): Type {
            return type
        }
    }
}