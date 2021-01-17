package com.jgeig001.kigga.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentMoreBinding
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.utils.FavClubChooser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_more.*
import javax.inject.Inject


@AndroidEntryPoint
class MoreFragment : Fragment() {

    @Inject
    lateinit var model: ModelWrapper

    private val moreViewModel: MoreViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Obtain binding
        val binding: FragmentMoreBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_more, container, false)

        // Bind layout with ViewModel
        binding.moreviewmodel = moreViewModel

        // LiveData needs the lifecycle owner
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFeedbackButton()

        setupNightModeButton()

        observeLiveData()

        more_selec_fav_btn.setOnClickListener { chooseFavClub() }

    }

    private fun setupFeedbackButton() {
        feedback_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.support_mail)))
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Kontakt")
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_mail_start))
            intent.type = "message/rfc822"
            startActivity(Intent.createChooser(intent, "choose an email client"))
        }
    }

    private fun setupNightModeButton() {

    }

    private fun observeLiveData() {
        moreViewModel.favClubLiveData.observe(
            viewLifecycleOwner,
            Observer {
                val club_string = FavClubChooser.getFavClubName(requireContext())
                if (club_string != "") {
                    more_favouriteClub.text = club_string
                    more_favouriteClub.visibility = View.VISIBLE
                } else {
                    more_favouriteClub.text = ""
                    more_favouriteClub.visibility = View.GONE
                }
            }
        )
    }

    private fun chooseFavClub() {
        val dialog =
            FavClubChooser.getLiveDataClubChooserDialog(
                requireContext(),
                model.getLiga(),
                moreViewModel.favClubLiveData
            )
        dialog.show()
    }

}
