package com.jetbrains.rider.plugins.sampleplugin

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import javax.swing.*
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import kotlin.random.Random

/**
 * Injects a parallax starfield background into every editor.
 * Don't forget to add this listener to your plugin.xml:
 * <extensions defaultExtensionNs="com.intellij">
 *   <editorFactoryListener implementation="com.jetbrains.rider.plugins.sampleplugin.StarryBackgroundFactoryListener"/>
 * </extensions>
 */
class StarryBackgroundFactoryListener : EditorFactoryListener {
    private val registry = mutableMapOf<Editor, Pair<StarFieldComponent, KeyAdapter>>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val content = editor.contentComponent
        // Create star field and key listener
        val field = StarFieldComponent(content.width, content.height)
        val keyListener = object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) = field.boostSpeed()
        }
        // Position and add to layered pane
        SwingUtilities.invokeLater {
            val layered = SwingUtilities.getAncestorOfClass(JLayeredPane::class.java, content) as? JLayeredPane
            if (layered != null) {
                field.setBounds(0, 0, content.width, content.height)
                layered.add(field, JLayeredPane.DEFAULT_LAYER)
                field.start()
                content.addKeyListener(keyListener)
                registry[editor] = field to keyListener
            }
        }
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        registry.remove(editor)?.let { (field, keyListener) ->
            SwingUtilities.invokeLater {
                field.stop()
                field.parent?.let { parent -> (parent as? JLayeredPane)?.remove(field) }
                editor.contentComponent.removeKeyListener(keyListener)
            }
        }
    }
}

private class StarFieldComponent(width: Int, height: Int) : JComponent() {
    private data class Star(var x: Float, var y: Float, val z: Float)
    private val stars = mutableListOf<Star>()
    private val timer: Timer
    private var speedBoost = 0f
    private val baseSpeed = 1f

    init {
        isOpaque = false
        // Initialize stars
        repeat(120) {
            stars += Star(
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height,
                z = Random.nextFloat() * 1.5f + 0.5f
            )
        }
        timer = Timer(16) {
            updateStars(width, height)
            repaint()
        }
    }

    fun start() = timer.start()
    fun stop() = timer.stop()
    fun boostSpeed() {
        speedBoost = 5f
    }

    private fun updateStars(w: Int, h: Int) {
        for (star in stars) {
            star.x -= (baseSpeed + speedBoost) / star.z
            if (star.x < 0) {
                star.x += w
                star.y = Random.nextFloat() * h
            }
        }
        // decay speedBoost towards zero
        speedBoost += (0f - speedBoost) * 0.05f
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g.create() as Graphics2D
        g2.color = Color.WHITE
        for (star in stars) {
            val size = (3f / star.z).toInt().coerceAtLeast(1)
            g2.fillOval(star.x.toInt(), star.y.toInt(), size, size)
        }
        g2.dispose()
    }
} 