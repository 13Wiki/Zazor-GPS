package com.gps.zazor.ui.settings.support

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.gps.zazor.BuildConfig
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentSupportBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.settings.support.di.injectViewModel
import com.gps.zazor.utils.viewBinding.viewBinding

/**
 * Writing to the author from inside the app, as the design has it: what it is about, what
 * happened, where to reply, and what to attach.
 *
 * There is no server behind this and there will not be one. The form composes a letter and hands
 * it to the person's own mail app, where they see exactly what is being sent and press send
 * themselves - which is also why the device details and the screenshot are switches rather than
 * something collected quietly.
 */
class SupportFragment : BaseFragment<SupportContract.State, SupportContract.Event>(
    R.layout.fragment_support
) {

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentSupportBinding::bind)

    private val pickScreenshot = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // A cancelled picker leaves the switch off: nothing is attached, and the screen must not
        // claim otherwise.
        viewModel.sendEvent(SupportContract.Event.ScreenshotPicked(uri))
    }

    override fun observeState(state: SupportContract.State?) {
        when (state) {
            is SupportContract.State.Form -> render(state)
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        topicPills().forEach { (pill, topic) ->
            pill.setOnClickListener {
                viewModel.sendEvent(SupportContract.Event.TopicSelected(topic))
            }
        }
        binding.tvDeviceHint.text = deviceSummary()
        binding.swDevice.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) {
                viewModel.sendEvent(SupportContract.Event.DeviceInfoToggled(checked))
            }
        }
        binding.swScreenshot.setOnCheckedChangeListener { button, checked ->
            if (!button.isPressed) return@setOnCheckedChangeListener
            if (checked) {
                pickScreenshot.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                viewModel.sendEvent(SupportContract.Event.ScreenshotPicked(null))
            }
        }
        binding.bSend.setOnClickListener { send() }
    }

    private fun topicPills() = listOf(
        binding.tvTopicBug to SupportContract.Topic.BUG,
        binding.tvTopicIdea to SupportContract.Topic.IDEA,
        binding.tvTopicPayment to SupportContract.Topic.PAYMENT,
        binding.tvTopicOther to SupportContract.Topic.OTHER
    )

    private fun render(form: SupportContract.State.Form) {
        topicPills().forEach { (pill, topic) -> pill.isSelected = topic == form.topic }
        binding.swDevice.isChecked = form.includeDeviceInfo
        binding.swScreenshot.isChecked = form.screenshot != null
        binding.tvScreenshotHint.text = form.screenshot
            ?.let { getString(R.string.support_screenshot_attached) }
            ?: getString(R.string.support_nothing_without_consent)
    }

    private fun currentForm(): SupportContract.State.Form? =
        viewModel.uiState.value as? SupportContract.State.Form

    private fun send() {
        val form = currentForm() ?: return
        val message = binding.etMessage.text?.toString().orEmpty().trim()
        if (message.isEmpty()) {
            binding.tilMessage.error = getString(R.string.support_message_required)
            return
        }
        binding.tilMessage.error = null
        val replyTo = binding.etEmail.text?.toString().orEmpty().trim()
        val body = buildString {
            append(message)
            if (replyTo.isNotEmpty()) {
                appendLine()
                appendLine()
                append(getString(R.string.support_reply_to, replyTo))
            }
            if (form.includeDeviceInfo) {
                appendLine()
                appendLine()
                append(deviceSummary())
            }
        }
        val subject = getString(R.string.support_subject, topicLabel(form.topic))
        try {
            startActivity(Intent.createChooser(letter(subject, body, form.screenshot), subject))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.feedback_no_mail_app, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * With nothing attached the letter goes through `mailto:`, which only mail apps answer. An
     * attachment needs a send intent instead, so the chooser is widened to anything that can send
     * an image - the address, subject and text ride along either way.
     */
    private fun letter(subject: String, body: String, attachment: Uri?): Intent {
        val address = getString(R.string.feedback_email)
        return if (attachment == null) {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:" + Uri.encode(address))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = MIME_IMAGE
                putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, attachment)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun topicLabel(topic: SupportContract.Topic): String = getString(
        when (topic) {
            SupportContract.Topic.BUG -> R.string.support_topic_bug
            SupportContract.Topic.IDEA -> R.string.support_topic_idea
            SupportContract.Topic.PAYMENT -> R.string.support_topic_payment
            SupportContract.Topic.OTHER -> R.string.support_topic_other
        }
    )

    /** Exactly what the switch promises, written out so the person can read it before agreeing. */
    private fun deviceSummary(): String = getString(
        R.string.support_device_summary,
        BuildConfig.VERSION_NAME,
        Build.MANUFACTURER + " " + Build.MODEL,
        Build.VERSION.RELEASE
    )

    private companion object {

        const val MIME_IMAGE = "image/*"
    }
}
