package com.rommelbendel.scanQ.impaired.visually

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SpeechSynthesisViewModel: ViewModel() {
    private val started: MutableLiveData<Boolean>? by lazy {
        MutableLiveData<Boolean>()
    }
    private val done: MutableLiveData<Boolean>? by lazy {
        MutableLiveData<Boolean>()
    }
    private val failed: MutableLiveData<Boolean>? by lazy {
        MutableLiveData<Boolean>()
    }

    fun hasStarted(): LiveData<Boolean>? {
        if (started == null) started?.value = false
        return started
    }
    fun isDone(): LiveData<Boolean>? {
        if (done == null) done?.value = false
        return done
    }
    fun hasFailed(): LiveData<Boolean>? {
        if (failed == null) failed?.value = false
        return failed
    }

    fun setStarted(started: Boolean) {
        this.started?.value = started
    }
    fun setDone(done: Boolean) {
        Log.e("SSVM", "synthesis done")
        this.done?.value = done
    }
    fun setFailed(failed: Boolean) {
        this.failed?.value = failed
    }
}