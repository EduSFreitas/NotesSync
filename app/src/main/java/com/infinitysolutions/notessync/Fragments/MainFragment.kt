package com.infinitysolutions.notessync.Fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.infinitysolutions.notessync.Adapters.NotesAdapter
import com.infinitysolutions.notessync.Contracts.Contract
import com.infinitysolutions.notessync.Contracts.Contract.Companion.CLOUD_DROPBOX
import com.infinitysolutions.notessync.Contracts.Contract.Companion.CLOUD_GOOGLE_DRIVE
import com.infinitysolutions.notessync.Contracts.Contract.Companion.IMAGE_CAPTURE_REQUEST_CODE
import com.infinitysolutions.notessync.Contracts.Contract.Companion.LIST_DEFAULT
import com.infinitysolutions.notessync.Contracts.Contract.Companion.NOTE_DEFAULT
import com.infinitysolutions.notessync.Contracts.Contract.Companion.PREF_ACCESS_TOKEN
import com.infinitysolutions.notessync.Contracts.Contract.Companion.PREF_CLOUD_TYPE
import com.infinitysolutions.notessync.Contracts.Contract.Companion.SHARED_PREFS_NAME
import com.infinitysolutions.notessync.MainActivity
import com.infinitysolutions.notessync.Model.Note
import com.infinitysolutions.notessync.R
import com.infinitysolutions.notessync.ViewModel.DatabaseViewModel
import com.infinitysolutions.notessync.ViewModel.MainViewModel
import kotlinx.android.synthetic.main.fragment_main.view.*

class MainFragment : Fragment() {
    private val TAG = "MainFragment"
    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_main, container, false)
        initDataBinding(rootView)

        rootView.search_button.setOnClickListener{
            Navigation.findNavController(rootView).navigate(R.id.action_mainFragment_to_searchFragment)
        }
        return rootView
    }

    private fun initDataBinding(rootView: View) {
        val databaseViewModel = ViewModelProviders.of(activity!!).get(DatabaseViewModel::class.java)
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)

        val notesRecyclerView = rootView.notes_recycler_view
        notesRecyclerView.layoutManager = LinearLayoutManager(activity!!)
        val toolbar = rootView.toolbar
        toolbar.title = "All"
        toolbar.inflateMenu(R.menu.main_fragment_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when(item.itemId){
                R.id.sync_menu_item ->{
                    syncFiles(rootView)
                }
            }
            true
        }
        mainViewModel.setToolbar(toolbar)

        rootView.new_note_button.setOnClickListener {
            mainViewModel.setShouldOpenEditor(true)
            mainViewModel.setSelectedNote(Note(-1L, "", "", 0, 0, "-1", NOTE_DEFAULT, false, null, -1L))
        }

        rootView.new_image_note_btn.setOnClickListener {
            val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(i, IMAGE_CAPTURE_REQUEST_CODE)
            //TODO: Open image selection dialog and then after image is received new image note is created
        }

        rootView.new_list_button.setOnClickListener{
            mainViewModel.setShouldOpenEditor(true)
            mainViewModel.setSelectedNote(Note(-1L, "", "", 0, 0, "-1", LIST_DEFAULT, false, null, -1L))
        }

        mainViewModel.getViewMode().observe(this, Observer { mode->
            if (mode != null){
                when (mode) {
                    1 -> {
                        toolbar.title = "All"
                        databaseViewModel.setViewMode(1)
                        rootView.empty_image.setImageResource(R.drawable.all_empty)
                        rootView.empty_text.text = getString(R.string.all_empty_message)
                    }
                    2 -> {
                        toolbar.title = "Notes"
                        databaseViewModel.setViewMode(2)
                        rootView.empty_image.setImageResource(R.drawable.notes_empty)
                        rootView.empty_text.text = getString(R.string.notes_empty_message)
                    }
                    3 -> {
                        toolbar.title = "To-do lists"
                        databaseViewModel.setViewMode(3)
                        rootView.empty_image.setImageResource(R.drawable.todo_empty)
                        rootView.empty_text.text = getString(R.string.todo_empty_message)
                    }
                    4 -> {
                        toolbar.title = "Archive"
                        databaseViewModel.setViewMode(4)
                        rootView.empty_image.setImageResource(R.drawable.archive_empty)
                        rootView.empty_text.text = getString(R.string.archived_empty_message)
                    }
                    5 -> {
                        toolbar.title = "Trash"
                        databaseViewModel.setViewMode(5)
                        rootView.empty_image.setImageResource(R.drawable.trash_empty)
                        rootView.empty_text.text = getString(R.string.trash_empty_message)
                    }
                }
            }
        })

        databaseViewModel.viewList.observe(this, Observer { viewList ->
            if (viewList != null && viewList.isNotEmpty()) {
                notesRecyclerView.visibility = VISIBLE
                rootView.empty_items.visibility = GONE
                notesRecyclerView.adapter = NotesAdapter(mainViewModel, databaseViewModel, viewList, context!!)
            }else{
                notesRecyclerView.visibility = GONE
                rootView.empty_items.visibility = VISIBLE
            }
        })

        mainViewModel.getShouldOpenEditor().observe(this, Observer {should ->
            if (should != null) {
                if (should) {
                    // If we don't put the navigation statement in try-catch block then app crashes due to unable to
                    // find navController. This is an issue in the Navigation components in Jetpack
                    try {
                        Navigation.findNavController(rootView).navigate(R.id.action_mainFragment_to_noteEditFragment)
                    } catch (e: Exception) { }
                }
            }
        })

        if (mainViewModel.intent == null) {
            val intent = activity?.intent
            if (intent != null && (intent.hasExtra(Intent.EXTRA_TEXT) || intent.hasExtra(Contract.WIDGET_BUTTON_EXTRA)
                        || intent.hasExtra(Contract.NOTE_ID_EXTRA))) {
                if (intent.flags != Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
                    mainViewModel.intent = intent
                    var text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (text != null) {
                        mainViewModel.setSelectedNote(Note(-1L, "", text, 0, 0, "-1", NOTE_DEFAULT, false, null, -1L))
                        mainViewModel.setShouldOpenEditor(true)
                    }

                    text = intent.getStringExtra(Contract.WIDGET_BUTTON_EXTRA)
                    if (text != null) {
                        if (text == Contract.WIDGET_NEW_NOTE) {
                            mainViewModel.setSelectedNote(Note(-1L, "", "", 0, 0, "-1", NOTE_DEFAULT, false, null, -1L))
                            mainViewModel.setShouldOpenEditor(true)
                        } else if (text == Contract.WIDGET_NEW_LIST) {
                            mainViewModel.setSelectedNote(Note(-1L, "", "", 0, 0, "-1", LIST_DEFAULT, false, null, -1L))
                            mainViewModel.setShouldOpenEditor(true)
                        }
                    }

                    val noteId = intent.getLongExtra(Contract.NOTE_ID_EXTRA, -1L)
                    if (noteId != -1L) {
                        val bundle = Bundle()
                        bundle.putLong("NOTE_ID", noteId)
                        Navigation.findNavController(activity!!, R.id.nav_host_fragment)
                            .navigate(R.id.noteEditFragment, bundle)
                    }
                }
            }
        }
    }

    private fun syncFiles(rootView: View){
        val prefs = activity?.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs != null){
            if(prefs.contains(PREF_CLOUD_TYPE)) {
                if (prefs.getInt(PREF_CLOUD_TYPE, CLOUD_GOOGLE_DRIVE) == CLOUD_DROPBOX) {
                    if (prefs.getString(PREF_ACCESS_TOKEN, null) != null)
                        mainViewModel.setSyncNotes(CLOUD_DROPBOX)
                    else
                        Navigation.findNavController(rootView).navigate(R.id.action_mainFragment_to_cloudPickerFragment)
                } else {
                    if (GoogleSignIn.getLastSignedInAccount(activity) != null)
                        mainViewModel.setSyncNotes(CLOUD_GOOGLE_DRIVE)
                    else
                        Navigation.findNavController(rootView).navigate(R.id.action_mainFragment_to_cloudPickerFragment)
                }
            }else{
                Navigation.findNavController(rootView).navigate(R.id.action_mainFragment_to_cloudPickerFragment)
            }
        }
    }
}
