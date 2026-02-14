package com.sameerasw.essentials.ime

import android.view.inputmethod.InputConnection
import java.util.Stack

enum class ActionType {
    INSERT, // We typed something. Undo = Delete it.
    DELETE  // We deleted something. Undo = Re-insert it.
}

data class HistoryAction(
    val type: ActionType,
    var text: String,
    val timestamp: Long
)

class UndoRedoManager {
    private val undoStack = Stack<HistoryAction>()
    private val redoStack = Stack<HistoryAction>()

    private val BATCH_TIMEOUT_MS = 2000L

    fun recordInsert(text: String) {
        val now = System.currentTimeMillis()
        redoStack.clear()

        if (undoStack.isNotEmpty()) {
            val top = undoStack.peek()
            val isRecent = (now - top.timestamp) < BATCH_TIMEOUT_MS
            
            if (top.type == ActionType.INSERT && isRecent) {
                top.text += text
                val updated = top.copy(text = top.text, timestamp = now)
                undoStack.pop()
                undoStack.push(updated)
                return
            }
        }
        
        undoStack.push(HistoryAction(ActionType.INSERT, text, now))
    }

    fun recordDelete(text: String) {
        if (text.isEmpty()) return
        val now = System.currentTimeMillis()
        redoStack.clear()
        
        if (undoStack.isNotEmpty()) {
            val top = undoStack.peek()
            val isRecent = (now - top.timestamp) < BATCH_TIMEOUT_MS
            
            if (top.type == ActionType.DELETE && isRecent) {
                top.text = text + top.text
                val updated = top.copy(text = top.text, timestamp = now)
                undoStack.pop()
                undoStack.push(updated)
                return
            }
        }

        undoStack.push(HistoryAction(ActionType.DELETE, text, now))
    }
    
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(ic: InputConnection?) {
        if (ic == null || undoStack.isEmpty()) return
        
        val action = undoStack.pop()
        redoStack.push(action)

        when (action.type) {
            ActionType.INSERT -> {
                ic.deleteSurroundingText(action.text.length, 0)
            }
            ActionType.DELETE -> {
                ic.commitText(action.text, 1)
            }
        }
    }

    fun redo(ic: InputConnection?) {
        if (ic == null || redoStack.isEmpty()) return
        
        val action = redoStack.pop()
        undoStack.push(action)

        when (action.type) {
            ActionType.INSERT -> {
                ic.commitText(action.text, 1)
            }
            ActionType.DELETE -> {
                ic.deleteSurroundingText(action.text.length, 0)
            }
        }
    }
}
