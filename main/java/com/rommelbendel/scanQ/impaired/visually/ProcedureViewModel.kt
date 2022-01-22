package com.rommelbendel.scanQ.impaired.visually

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProcedureViewModel : ViewModel() {
    private val answers: MutableLiveData<ArrayList<String>>? by lazy {
        MutableLiveData<ArrayList<String>>()
    }
    private val completed: MutableLiveData<Boolean>? by lazy {
        MutableLiveData<Boolean>()
    }
    private val terminated: MutableLiveData<Boolean>? by lazy {
        MutableLiveData<Boolean>()
    }
    fun getAnswers(): LiveData<ArrayList<String>>? {
        if (answers == null) {
            answers?.value = ArrayList()
        }
        return answers
    }
    fun isCompleted(): LiveData<Boolean>? {
        return completed
    }
    fun isTerminated(): LiveData<Boolean>? {
        return terminated
    }
    fun addAnswer(answer: String) {
        var newAnswers: ArrayList<String>? = answers?.value
        if (newAnswers == null) {
            newAnswers = ArrayList()
        }
        newAnswers.add(answer)
        answers?.value = newAnswers
    }
    fun setCompleted(completed: Boolean) {
        this.completed?.value = completed
    }
    fun setTerminated(terminated: Boolean) {
        this.terminated?.value = terminated
    }

    fun clearVM() {
        answers?.value = null
        completed?.value = null
        terminated?.value = null
    }
}