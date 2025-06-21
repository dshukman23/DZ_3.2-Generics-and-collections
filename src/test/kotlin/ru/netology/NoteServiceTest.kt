package ru.netology

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NoteServiceTest {

    private lateinit var service: NoteService

    @Before
    fun setUp() {
        service = NoteService()
    }

    @Test
    fun testAddNote() {
        val id = service.add("Заголовок", "Текст заметки")
        assertTrue(id > 0)
    }

    @Test(expected = NotFoundException::class)
    fun testDeleteNonExistentNote_throwsNotFound() {
        service.delete(999)
    }

    @Test(expected = PermissionDeniedException::class)
    fun testEditDeletedNote_throwsPermissionDenied() {
        val id = service.add("Старый заголовок", "Старый текст")
        service.delete(id)
        service.edit(id, "Новый заголовок", "Новый текст")
    }

    @Test
    fun testCreateAndDeleteComment_success() {
        val noteId = service.add("Заметка", "Тело")
        val commentId = service.createComment(noteId, "Мой комментарий")
        assertEquals(1, commentId)

        try {
            service.deleteComment(commentId)
        } catch (e: Exception) {
            fail("Удаление комментария не должно вызывать исключение: ${e.message}")
        }
    }

    @Test(expected = PermissionDeniedException::class)
    fun testDeleteAlreadyDeletedComment_throwsPermissionDenied() {
        val noteId = service.add("Заметка", "Тело")
        val commentId = service.createComment(noteId, "Мой комментарий")
        service.deleteComment(commentId)
        service.deleteComment(commentId)
    }

    @Test
    fun testRestoreComment_success() {
        val noteId = service.add("Тестовая заметка", "Тест")
        val commentId = service.createComment(noteId, "Привет!")
        service.deleteComment(commentId)

        try {
            service.restoreComment(commentId)
        } catch (e: Exception) {
            fail("Восстановление комментария не должно вызывать исключение: ${e.message}")
        }
    }

    @Test(expected = PermissionDeniedException::class)
    fun testRestoreNonDeletedComment_throwsPermissionDenied() {
        val noteId = service.add("Тестовая заметка", "Тест")
        val commentId = service.createComment(noteId, "Привет!")
        service.restoreComment(commentId)
    }

    @Test
    fun testGetComments_returnsOnlyNonDeleted() {
        val noteId = service.add("Заметка", "Тело")
        val c1 = service.createComment(noteId, "Комментарий 1")
        val c2 = service.createComment(noteId, "Комментарий 2")
        service.deleteComment(c2)

        val result = service.getComments(noteId)
        assertEquals(1, result.size)
        assertEquals(c1, result[0].id)
    }

    @Test
    fun testEditComment_success() {
        val noteId = service.add("Заметка", "Тело")
        val commentId = service.createComment(noteId, "Исходный комментарий")
        try {
            val result = service.editComment(commentId, ownerId = 1, "Обновлённый текст")
            assertEquals(1, result)
        } catch (e: Exception) {
            fail("Редактирование комментария не должно вызывать исключение: ${e.message}")
        }
    }

    @Test(expected = PermissionDeniedException::class)
    fun testEditDeletedComment_throwsPermissionDenied() {
        val noteId = service.add("Заметка", "Тело")
        val commentId = service.createComment(noteId, "Исходный комментарий")
        service.deleteComment(commentId)
        service.editComment(commentId, ownerId = 1, "Обновлённый текст")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEditComment_messageTooShort_throwsError() {
        val noteId = service.add("Заметка", "Тело")
        val commentId = service.createComment(noteId, "Исходный комментарий")
        service.editComment(commentId, ownerId = 1, "A")
    }

    @Test
    fun testGet_notesWithOffsetCountSort() {
        service.add("Заметка 1", "Текст 1") // id=1
        Thread.sleep(100)
        service.add("Заметка 2", "Текст 2") // id=2
        Thread.sleep(100)
        service.add("Заметка 3", "Текст 3") // id=3

        val resultDesc = service.get(userId = 1, count = 2, sort = 0)
        assertEquals(2, resultDesc.size)
        assertEquals(2, resultDesc[1].id)
        assertEquals(2, resultDesc[1].id)

        val resultAsc = service.get(userId = 1, offset = 1, count = 2, sort = 1)
        assertEquals(2, resultAsc.size)
        assertEquals(2, resultAsc[0].id)
        assertEquals(3, resultAsc[1].id)
    }

    @Test
    fun testGetById_returnsCorrectFields() {
        service.add("Секретная заметка", "Супер секретный текст")
        val data = service.getById(noteId = 1, ownerId = 1)

        assertEquals(1, data["id"])
        assertEquals(1, data["owner_id"])
        assertEquals("Секретная заметка", data["title"])
        assertEquals("Супер секретный текст", data["text"])
        assertEquals(0, data["privacy"])
        assertEquals(0, data["comment_privacy"])
        assertEquals(1, data["can_comment"])
    }

    @Test
    fun testGetById_canCommentDependsOnPrivacy() {
        val noteId = service.add("Приватная заметка", "Ограниченный доступ")
        service.updateNotePrivacy(noteId, 1)

        val data = service.getById(noteId = noteId, ownerId = 1)
        assertEquals(1, data["can_comment"])
    }
}