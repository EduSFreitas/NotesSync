package com.infinitysolutions.notessync.viewmodel

import android.content.Intent
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.infinitysolutions.notessync.model.ImageData
import com.infinitysolutions.notessync.model.Note
import com.infinitysolutions.notessync.util.Event

class MainViewModel: ViewModel(){
    private var selectedNote: Note? = null
    private val multiSelectCount = MutableLiveData<Int>()
    private val selectedColor = MutableLiveData<Int>()
    private val shouldOpenEditor = MutableLiveData<Boolean>()
    private val imagesList = ArrayList<ImageData>()
    private val openImageView = MutableLiveData<Event<Int>>()
    private val mToolbar = MutableLiveData<Toolbar?>()
    private val viewMode = MutableLiveData<Int>()
    private val refreshImagesList = MutableLiveData<Event<Boolean>>()
    var noteType: Int? = null
    private var currentPhotoPath: String? = null
    var intent: Intent? = null
    var reminderTime = -1L

    init{
        viewMode.value = 1
    }

    fun setMultiSelectCount(value: Int){
        multiSelectCount.value = value
    }

    fun setRefreshImagesList(value: Boolean){
        refreshImagesList.value = Event(value)
    }

    fun setCurrentPhotoPath(photoPath: String){
        currentPhotoPath = photoPath
    }

    fun setViewMode(mode: Int){
        viewMode.value = mode
    }

    fun setToolbar(toolbar: Toolbar?){
        mToolbar.value = toolbar
    }

    fun setSelectedNote(note: Note?){
        selectedNote = note
    }

    fun setSelectedColor(color: Int?){
        if (color == null)
            selectedColor.value = 0
        else
            selectedColor.value = color
    }

    fun getSelectedColor() : LiveData<Int>{
        if (selectedColor.value == null)
            selectedColor.value = 0
        return selectedColor
    }

    fun setShouldOpenEditor(shouldOpen: Boolean){
        shouldOpenEditor.value = shouldOpen
    }

    fun setImagesList(list: ArrayList<ImageData>?){
        imagesList.clear()
        if(list != null)
            for(item in list)
                imagesList.add(item)
    }

    fun addImageToImageList(image: ImageData){
        imagesList.add(image)
    }

    fun setOpenImageView(imageId: Int){
        openImageView.value = Event(imageId)
    }

    fun getMultiSelectCount(): LiveData<Int> = multiSelectCount
    fun getRefreshImagesList(): LiveData<Event<Boolean>> = refreshImagesList
    fun getCurrentPhotoPath(): String? = currentPhotoPath
    fun getViewMode(): LiveData<Int> = viewMode
    fun getToolbar(): LiveData<Toolbar?> = mToolbar
    fun getSelectedNote(): Note? = selectedNote
    fun getShouldOpenEditor(): LiveData<Boolean> = shouldOpenEditor
    fun getImagesList(): ArrayList<ImageData> = imagesList
    fun getOpenImageView(): LiveData<Event<Int>> = openImageView

}