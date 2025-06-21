package ru.netology

class NotFoundException(message: String) : Exception(message)
class PermissionDeniedException(message: String) : Exception(message)

data class Note(
    val id: Int,
    val ownerId: Int,
    var title: String,
    var text: String,
    var privacy: Int = 0,
    var commentPrivacy: Int = 0,
    var deleted: Boolean = false
)

data class Comment(
    val id: Int,
    val noteId: Int,
    val ownerId: Int,
    val userId: Int,
    var message: String,
    val replyTo: Int? = null,
    val date: Long = System.currentTimeMillis(),
    var deleted: Boolean = false
)

class NoteService {
    private var currentUserId: Int = 1
    private val notes = mutableListOf<Note>()
    private val comments = mutableListOf<Comment>()
    private var noteCounter = 1
    private var commentCounter = 1

    // add
    fun add(title: String, text: String): Int {
        val note = Note(
            id = noteCounter++,
            ownerId = currentUserId,
            title = title,
            text = text
        )
        notes.add(note)
        return note.id
    }

    // edit
    fun edit(noteId: Int, title: String, text: String): Boolean {
        val note = notes.find { it.id == noteId && it.ownerId == currentUserId }
            ?: throw NotFoundException("Заметка не найдена")
        if (note.deleted) throw PermissionDeniedException("Нельзя редактировать удалённую заметку")

        note.title = title
        note.text = text
        return true
    }

    // delete
    fun delete(noteId: Int): Boolean {
        val note = notes.find { it.id == noteId && it.ownerId == currentUserId }
            ?: throw NotFoundException("Заметка не найдена")
        note.deleted = true
        return true
    }

    // createComment
    fun createComment(noteId: Int, message: String, replyTo: Int? = null): Int {
        val note = notes.find { it.id == noteId && !it.deleted }
            ?: throw NotFoundException("Заметка не найдена или удалена")
        val comment = Comment(
            id = commentCounter++,
            noteId = noteId,
            ownerId = note.ownerId,
            userId = currentUserId,
            message = message,
            replyTo = replyTo
        )
        comments.add(comment)
        return comment.id
    }

    // deleteComment
    fun deleteComment(commentId: Int): Boolean {
        val comment = comments.find { it.id == commentId }
            ?: throw NotFoundException("Комментарий не найден")
        if (comment.userId != currentUserId) throw PermissionDeniedException("Это не ваш комментарий")
        if (comment.deleted) throw PermissionDeniedException("Комментарий уже удалён")
        comment.deleted = true
        return true
    }

    // restoreComment
    fun restoreComment(commentId: Int): Boolean {
        val comment = comments.find { it.id == commentId }
            ?: throw NotFoundException("Комментарий не найден")
        if (comment.userId != currentUserId) throw PermissionDeniedException("Это не ваш комментарий")
        if (!comment.deleted) throw PermissionDeniedException("Комментарий не удален")
        comment.deleted = false
        return true
    }

    // getComments
    fun getComments(noteId: Int): List<Comment> {
        val note = notes.find { it.id == noteId && !it.deleted }
            ?: throw NotFoundException("Заметка не найдена или удалена")
        return comments.filter { it.noteId == noteId && !it.deleted }
    }

    // editComment
    fun editComment(commentId: Int, ownerId: Int, message: String): Int {
        if (message.length < 2) throw IllegalArgumentException("Сообщение должно быть не короче 2 символов")
        val comment = comments.find { it.id == commentId && it.ownerId == ownerId }
            ?: throw NotFoundException("Комментарий не найден или владелец не совпадает")
        if (comment.userId != currentUserId) throw PermissionDeniedException("Это не ваш комментарий")
        if (comment.deleted) throw PermissionDeniedException("Нельзя редактировать удалённый комментарий")

        comment.message = message
        return 1
    }

    // get
    fun get(noteIds: List<Int>? = null, userId: Int? = null, offset: Int = 0, count: Int = 10, sort: Int = 0): List<Note> {
        var result = notes.toMutableList()

        if (noteIds != null) {
            result = result.filter { it.id in noteIds }.toMutableList()
        }

        if (userId != null) {
            result = result.filter { it.ownerId == userId }.toMutableList()
        }

        result = result.filter { !it.deleted }.toMutableList()

        result = when (sort) {
            0 -> result.sortedByDescending { it.date }.toMutableList()
            1 -> result.sortedBy { it.date }.toMutableList()
            else -> result
        }

        return result.drop(offset).take(count)
    }

    private val Note.date get() = comments
        .filter { it.noteId == id && !it.deleted }
        .maxOfOrNull { it.date } ?: 0L

    // getById
    fun getById(noteId: Int, ownerId: Int, needWiki: Boolean = false): Map<String, Any> {
        val note = notes.find { it.id == noteId && it.ownerId == ownerId && !it.deleted }
            ?: throw NotFoundException("Заметка не найдена или удалена")

        val canComment = if (currentUserId == ownerId) 1
        else when (note.commentPrivacy) {
            0 -> 1
            1 -> if (isFriend(currentUserId, ownerId)) 1 else 0
            2 -> if (isFriend(currentUserId, ownerId) || isFriendOfFriend(currentUserId, ownerId)) 1 else 0
            3 -> if (currentUserId == ownerId) 1 else 0
            else -> 0
        }

        return mapOf(
            "id" to note.id,
            "owner_id" to note.ownerId,
            "title" to note.title,
            "text" to note.text,
            "privacy" to note.privacy,
            "comment_privacy" to note.commentPrivacy,
            "can_comment" to canComment
        )
    }

    fun updateNotePrivacy(noteId: Int, commentPrivacy: Int) {
        val note = notes.find { it.id == noteId } ?: throw NotFoundException("Заметка не найдена")
        note.commentPrivacy = commentPrivacy
    }

    private fun isFriend(a: Int, b: Int): Boolean = a == b + 1 || b == a + 1
    private fun isFriendOfFriend(a: Int, b: Int): Boolean = a == b + 2 || b == a + 2
}