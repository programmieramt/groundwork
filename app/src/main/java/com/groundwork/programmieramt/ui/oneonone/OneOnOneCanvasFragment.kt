package com.groundwork.programmieramt.ui.oneonone

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.groundwork.programmieramt.databinding.FragmentOneOnOneCanvasBinding
import com.groundwork.programmieramt.pen.DrawingFragment
import com.groundwork.programmieramt.pen.FormTemplate
import com.groundwork.programmieramt.util.toGermanDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OneOnOneCanvasFragment : DrawingFragment() {

    private var _binding: FragmentOneOnOneCanvasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OneOnOneViewModel by viewModels()

    private var sessionId = 0L

    override fun provideSurfaceView(): SurfaceView = binding.surfaceView
    override fun provideTemplateView(): ImageView = binding.templateImageView

    override fun buildTemplateBitmap(width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        FormTemplate.drawOneOnOne(c, width, height)
        return bmp
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOneOnOneCanvasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionId = arguments?.getLong(ARG_SESSION_ID, 0L) ?: 0L

        binding.penToolbar.onToolSelected = { t -> setTool(t.color, t.strokeWidth, t.isMarker, t.isEraser) }
        binding.penToolbar.currentTool().let { t -> setTool(t.color, t.strokeWidth, t.isMarker, t.isEraser) }

        binding.btnEditMeta.setOnClickListener {
            findNavController().navigate(
                com.groundwork.programmieramt.R.id.action_canvas_to_meta,
                OneOnOneMetaFragment.args(sessionId)
            )
        }

        binding.btnSave.setOnClickListener { save() }

        if (sessionId > 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getById(sessionId)?.let { s ->
                    updateHeader(s.memberName, s.datum, s.titel)
                    loadStrokes(s.strokes)
                }
            }
        }

        initSurface()
    }

    override fun onStrokesChanged() {
        save()
    }

    private fun save() {
        if (sessionId == 0L) return
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getById(sessionId)?.let { existing ->
                viewModel.save(existing.copy(
                    strokes = getStrokesJson(),
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    private fun updateHeader(name: String, datum: Long, titel: String) {
        val parts = buildList {
            if (name.isNotBlank()) add(name)
            add(datum.toGermanDate())
            if (titel.isNotBlank()) add(titel)
        }
        binding.tvMeta.text = parts.joinToString(" · ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SESSION_ID = "session_id"
        fun args(sessionId: Long) = bundleOf(ARG_SESSION_ID to sessionId)
    }
}
