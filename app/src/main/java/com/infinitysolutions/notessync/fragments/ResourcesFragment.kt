package com.infinitysolutions.notessync.fragments


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.infinitysolutions.notessync.R
import kotlinx.android.synthetic.main.fragment_resources.view.*

class ResourcesFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_resources, container, false)
        setupView(rootView)
        return rootView
    }

    private fun setupView(rootView: View) {
        val toolbar = rootView.toolbar
        toolbar.title = getString(R.string.open_source)
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        rootView.app_compat.setOnClickListener {
            openLink("https://developer.android.com/jetpack/androidx/releases/appcompat")
        }

        rootView.recycler_view.setOnClickListener {
            openLink("https://developer.android.com/jetpack/androidx/releases/recyclerview")
        }

        rootView.constraint_layout.setOnClickListener {
            openLink("https://developer.android.com/reference/androidx/constraintlayout/widget/ConstraintLayout")
        }

        rootView.lifecycle.setOnClickListener {
            openLink("https://developer.android.com/jetpack/androidx/releases/lifecycle")
        }

        rootView.navigation.setOnClickListener {
            openLink("https://developer.android.com/guide/navigation")
        }

        rootView.room.setOnClickListener {
            openLink("https://developer.android.com/jetpack/androidx/releases/room")
        }

        rootView.workmanager.setOnClickListener {
            openLink("https://developer.android.com/topic/libraries/architecture/workmanager")
        }

        rootView.bottom_sheet.setOnClickListener {
            openLink("https://material.io/develop/android/components/bottom-sheet-behavior/")
        }

        rootView.kotlin.setOnClickListener {
            openLink("https://kotlinlang.org/")
        }

        rootView.coroutines.setOnClickListener {
            openLink("https://developer.android.com/kotlin/coroutines")
        }

        rootView.checklist.setOnClickListener {
            openLink("https://github.com/KumarManas04/ChecklistView")
        }

        rootView.icons.setOnClickListener {
            openLink("https://icons8.com/icons/")
        }

        rootView.flat_icon.setOnClickListener {
            openLink("https://www.flaticon.com/")
        }

        rootView.drive_rest.setOnClickListener {
            openLink("https://developers.google.com/drive/api/v3/about-sdk")
        }

        rootView.dropbox_api.setOnClickListener {
            openLink("https://www.dropbox.com/developers/documentation/java")
        }

        rootView.google_oauth.setOnClickListener {
            openLink("https://developers.google.com/identity/protocols/OAuth2")
        }

        rootView.touch_image_view.setOnClickListener {
            openLink("https://github.com/MikeOrtiz/TouchImageView")
        }
    }

    private fun openLink(link: String){
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        if (browserIntent.resolveActivity(activity!!.packageManager) != null)
            startActivity(browserIntent)
        else
            Toast.makeText(activity, getString(R.string.toast_no_browser), Toast.LENGTH_SHORT).show()
    }
}
