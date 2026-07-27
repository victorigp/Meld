package com.metrolist.music.ui.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import androidx.compose.ui.node.DelegatableNode

fun isAndroidTv(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

class TvFocusIndication(
    private val baseIndicationFactory: IndicationNodeFactory
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return TvFocusNode(baseIndicationFactory.create(interactionSource), interactionSource)
    }

    override fun equals(other: Any?): Boolean =
        other === this || (other is TvFocusIndication && baseIndicationFactory == other.baseIndicationFactory)

    override fun hashCode(): Int = baseIndicationFactory.hashCode()
}

class TvFocusNode(
    private val baseNode: DelegatableNode,
    private val interactionSource: InteractionSource
) : DelegatingNode(), DrawModifierNode {

    private var isFocused = false

    init {
        delegate(baseNode)
    }

    override fun onAttach() {
        super.onAttach()
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> isFocused = true
                    is FocusInteraction.Unfocus -> isFocused = false
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        
        if (isFocused) {
            drawRect(
                color = Color.White,
                alpha = 0.2f,
            )
            drawRect(
                color = Color.White,
                alpha = 0.8f,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
