package com.infinitysolutions.notessync.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.*
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.gson.Gson
import com.infinitysolutions.notessync.contracts.Contract
import com.infinitysolutions.notessync.contracts.Contract.Companion.IMAGE_ARCHIVED
import com.infinitysolutions.notessync.contracts.Contract.Companion.IMAGE_DEFAULT
import com.infinitysolutions.notessync.contracts.Contract.Companion.IMAGE_DELETED
import com.infinitysolutions.notessync.contracts.Contract.Companion.IMAGE_LIST_ARCHIVED
import com.infinitysolutions.notessync.contracts.Contract.Companion.IMAGE_LIST_DEFAULT
import com.infinitysolutions.notessync.contracts.Contract.Companion.IMAGE_LIST_TRASH
import com.infinitysolutions.notessync.contracts.Contract.Companion.IMAGE_TRASH
import com.infinitysolutions.notessync.contracts.Contract.Companion.NOTE_DELETED
import com.infinitysolutions.notessync.contracts.Contract.Companion.ORDER_BY_UPDATED
import com.infinitysolutions.notessync.contracts.Contract.Companion.ORDER_DESC
import com.infinitysolutions.notessync.contracts.Contract.Companion.PREF_ORDER
import com.infinitysolutions.notessync.contracts.Contract.Companion.PREF_ORDER_BY
import com.infinitysolutions.notessync.contracts.Contract.Companion.PREF_SYNC_QUEUE
import com.infinitysolutions.notessync.contracts.Contract.Companion.SHARED_PREFS_NAME
import com.infinitysolutions.notessync.fragments.NotesWidget
import com.infinitysolutions.notessync.model.*
import com.infinitysolutions.notessync.repository.NotesRepository
import com.infinitysolutions.notessync.util.WorkSchedulerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList

class DatabaseViewModel(application: Application) : AndroidViewModel(application) {
    private val query = MutableLiveData<SimpleSQLiteQuery>()
    private val viewMode = MutableLiveData<Int>()
    private var orderBy = ORDER_BY_UPDATED
    private var order = ORDER_DESC
    private val notesDao: NotesDao = NotesRoomDatabase.getDatabase(application).notesDao()
    private val imagesDao: ImagesDao = NotesRoomDatabase.getDatabase(application).imagesDao()
    private val repository: NotesRepository = NotesRepository(notesDao)
    val searchResultList: LiveData<List<Note>> = Transformations.switchMap(query) { searchQuery ->
        notesDao.getSearchResult(searchQuery)
    }
    val viewList: LiveData<List<Note>> = Transformations.switchMap(viewMode) { mode ->
        when (mode) {
            1 -> repository.getNotesList(orderBy, order)
            2 -> repository.getArchiveList(orderBy, order)
            3 -> repository.getTrashList(orderBy, order)
            else -> null
        }
    }

    init {
        query.value = SimpleSQLiteQuery("SELECT * FROM notes_table WHERE type != 0 AND type != 5 AND type != 6 AND type != 9 AND type != 10 AND type != 13")
        viewMode.value = 1
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        orderBy = prefs.getString(PREF_ORDER_BY, ORDER_BY_UPDATED)?: ORDER_BY_UPDATED
        order = prefs.getString(PREF_ORDER, ORDER_DESC)?: ORDER_DESC
    }

    fun setSearchQuery(searchQuery: String) {
        val tempList = searchQuery.split("\\s+".toRegex())
        val list = ArrayList<String>()
        for(str in tempList){
            if(str.trim().isNotEmpty())
                list.add(str)
        }

        if(list.isEmpty()) {
            query.value = SimpleSQLiteQuery("SELECT * FROM notes_table WHERE type != 0 AND type != 5 AND type != 6 AND type != 9 AND type != 10 AND type != 13")
            return
        }
        val sB = StringBuilder("SELECT * FROM notes_table WHERE type != 0 AND type != 5 AND type != 6 AND type != 9 AND type != 10 AND type != 13 AND (")
        list.forEachIndexed { index, str ->
            if (index != 0)
                sB.append(" OR ")
            sB.append("title LIKE '%$str%' OR content LIKE '%$str%'")
        }

        sB.append(")")
        val queryItem = SimpleSQLiteQuery(sB.toString())
        query.value = queryItem
    }

    fun setViewMode(mode: Int) {
        viewMode.value = mode
    }

    fun setOrder(order: String, orderBy: String){
        this.order = order
        this.orderBy = orderBy
        val vM = viewMode.value
        viewMode.value = vM
    }

    fun insert(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteId = repository.insert(note)
            withContext(Dispatchers.Main) {
                val context = getApplication<Application>().applicationContext
                updateWidgets(context)
                val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
                var set = prefs.getStringSet(PREF_SYNC_QUEUE, null)
                if(set == null)
                   set = hashSetOf(noteId.toString())
                else
                    set.add(noteId.toString())

                val editor = prefs.edit()
                editor.putStringSet(PREF_SYNC_QUEUE, set)
                editor.commit()
                WorkSchedulerHelper().syncNotes(false, context)
            }
        }
    }

    fun makeCopy(note: Note?, noteType: Int?, noteTitle: String, noteContent: String) {
        if(noteType != null && note != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val currentTime = Calendar.getInstance().timeInMillis
                when (noteType) {
                    IMAGE_DEFAULT, IMAGE_ARCHIVED, IMAGE_LIST_DEFAULT, IMAGE_LIST_ARCHIVED -> {
                        val imageNoteContent = Gson().fromJson(noteContent, ImageNoteContent::class.java)
                        val idsList = imageNoteContent.idList
                        val imageDataList = getImagesByIds(idsList)
                        var bitmap: Bitmap
                        var file: File
                        val newIdList = ArrayList<Long>()
                        for (imageData in imageDataList) {
                            file = File(imageData.imagePath)
                            bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            newIdList.add(insertImage(bitmap).imageId!!)
                            bitmap.recycle()
                        }
                        val newNoteContent = Gson().toJson(ImageNoteContent(imageNoteContent.noteContent, newIdList))
                        val newNote = Note(
                            null,
                            noteTitle,
                            newNoteContent,
                            currentTime,
                            currentTime,
                            "-1",
                            noteType,
                            false,
                            note.noteColor,
                            -1L
                        )
                        insert(newNote)
                    }
                    else -> {
                        val newNote = Note(
                            null,
                            noteTitle,
                            noteContent,
                            currentTime,
                            currentTime,
                            "-1",
                            noteType,
                            false,
                            note.noteColor,
                            -1L
                        )
                        insert(newNote)
                    }
                }
            }
        }
    }

    fun insertImage(): ImageData{
        Log.d("DBVM", "Insert Image called")
        val path = getApplication<Application>().applicationContext.filesDir.toString()

        val time = Calendar.getInstance().timeInMillis
        val file = File(path, "$time.jpg")
        val imageData = ImageData(null, file.absolutePath, time, time, null)
        imageData.imageId = imagesDao.insert(imageData)
        return imageData
    }

    private fun insertImage(imageBitmap: Bitmap): ImageData {
        val path = getApplication<Application>().applicationContext.filesDir.toString()

        val time = Calendar.getInstance().timeInMillis
        val file = File(path, "$time.jpg")
        try {
            val fos = FileOutputStream(file)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val imageData = ImageData(null, file.absolutePath, time, time, null)
        imageData.imageId = imagesDao.insert(imageData)
        return imageData
    }

    fun deleteImage(id: Long, path: String) {
        val file = File(path)
        if (file.exists())
            file.delete()
        imagesDao.deleteImageById(id)
    }

    fun deleteImagesByIds(idList: ArrayList<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val images = getImagesByIds(idList)
            for (image in images)
                deleteImage(image.imageId!!, image.imagePath)
        }
    }

    fun deleteNote(note: Note) {
        val prefs = getApplication<Application>().applicationContext.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        val isLoggedIn = prefs != null && prefs.contains(Contract.PREF_CLOUD_TYPE)
        val noteType = if (note.noteType == IMAGE_TRASH || note.noteType == IMAGE_LIST_TRASH) {
            val imageNoteContent = Gson().fromJson(note.noteContent, ImageNoteContent::class.java)
            deleteImagesByIds(imageNoteContent.idList)
            IMAGE_DELETED
        } else {
            NOTE_DELETED
        }
        if(isLoggedIn)
            changeNoteType(note, noteType)
        else {
            viewModelScope.launch(Dispatchers.IO) {
                notesDao.deleteNoteById(note.nId!!)
            }
        }
    }

    private fun changeNoteType(note: Note, noteType: Int) {
        insert(
            Note(
                note.nId,
                note.noteTitle,
                note.noteContent,
                note.dateCreated,
                Calendar.getInstance().timeInMillis,
                note.gDriveId,
                noteType,
                note.synced,
                note.noteColor,
                note.reminderTime
            )
        )
    }

    private fun updateWidgets(context: Context) {
        val intent = Intent(context, NotesWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, NotesWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    fun getNoteById(nId: Long): Note = notesDao.getNoteById(nId)
    fun getImagesByIds(idList: ArrayList<Long>): ArrayList<ImageData> = ArrayList(imagesDao.getImagesByIds(idList))
    fun getImagePathById(id: Long): String = imagesDao.getImagePathById(id)
}