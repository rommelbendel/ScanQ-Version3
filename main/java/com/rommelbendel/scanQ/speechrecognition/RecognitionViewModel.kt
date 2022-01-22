package com.rommelbendel.scanQ.speechrecognition

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecognitionViewModel : ViewModel() {
    private val currentResult: MutableLiveData<List<String>>? by lazy {
        MutableLiveData<List<String>>()
    }
    private val recognitionCompleted: MutableLiveData<Boolean>? by lazy {
        MutableLiveData<Boolean>()
    }

    private val supportedLanguages: MutableLiveData<List<String>>? by lazy {
        MutableLiveData<List<String>>()
    }

    fun getCurrentResult(): LiveData<List<String>>? {
        return currentResult
    }

    fun setCurrentResult(result: List<String>?) {
        currentResult?.value = result
    }

    fun isRecognitionCompleted(): LiveData<Boolean>? {
        return recognitionCompleted
    }

    fun setRecognitionCompleted(completed: Boolean?) {
        recognitionCompleted?.value = completed
    }

    fun getSupportedLanguages(): LiveData<List<String>>? {
        return supportedLanguages
    }

    fun setSupportedLanguages(supported: List<String>?) {
        supportedLanguages?.value = supported
    }
}