package core

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class ReplyClassTest {

    private val bot = mockk<Bot>(relaxed = true)
    private val chatId = ChatId.fromId(123L)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        currState = States.GeneralMenu
    }

    @Test
    @DisplayName("Простая кнопка: смена состояния и вызов функции")
    fun testBaseButton() {
        var funcCalled = false
        val button = Button(
            name = "Тест",
            func = { funcCalled = true; "Сообщение" },
            stateChange = States.MemeMenu
        )

        val result = button.detect()

        assertEquals("Тест", button.getText())
        assertEquals("Сообщение", result)
        assertEquals(true, funcCalled)
        assertEquals(States.MemeMenu, currState)
    }

    @Test
    @DisplayName("BoolButton: переключение флага и текста")
    fun testBoolButton() {
        val button = BoolButton(name = "Уведомления", flag = true)

        assertTrue(button.getText().contains("✅"))

        button.detect()
        assertFalse(button.flag)
        assertTrue(button.getText().contains("❌"))

        button.detect()
        assertTrue(button.flag)
        assertTrue(button.getText().contains("✅"))
    }

    @Test
    @DisplayName("ChooseButton: циклическое переключение вариантов")
    fun testChooseButton() {
        val options = mapOf(
            "Раз" to { "1" },
            "Два" to { "2" },
            "Три" to { "3" }
        )
        val button = ChooseButton(name = "Выбор", list = options)

        assertEquals("[Раз] Выбор", button.getText())

        button.detect()
        assertEquals("[Два] Выбор", button.getText())

        button.detect()
        button.detect()
        assertEquals("[Раз] Выбор", button.getText())
    }

    @Test
    @DisplayName("ReplyClass: автоматическое форматирование (пагинация)")
    fun testReplyClassFormatting() {
        val buttons = mutableListOf<MutableList<Button>>()
        repeat(8) { i ->
            buttons.add(mutableListOf(Button("Кнопка $i")))
        }

        val reply = ReplyClass(
            keyboard = buttons,
            globalCommand = "/test",
            state = States.GeneralMenu,
            startFunc = { "Старт" },
            thresholdCountButtons = 6
        )

        assertTrue(reply.groupKeyboards.size > 1)
        val hasNext = reply.keyboard.any { row -> row.any { it.name == "➡️ Дальше" } }
        assertTrue(hasNext)
    }

    @Test
    @DisplayName("ReplyClass: обработка входящего текста (processing)")
    fun testReplyProcessing() {
        val testButton = Button("Жми меня", func = { "Нажато" })
        val reply = ReplyClass(
            keyboard = mutableListOf(mutableListOf(testButton)),
            globalCommand = "/start_test",
            state = States.CollectionsMenu,
            startFunc = { "Начальный экран" }
        )

        currState = States.GeneralMenu
        val (msg1, isBtn1) = reply.processing("/start_test")
        assertEquals(States.CollectionsMenu, currState)
        assertFalse(isBtn1)

        val (msg2, isBtn2) = reply.processing("Жми меня")
        assertEquals("Нажато", msg2)
        assertTrue(isBtn2)

        val (msg3, isBtn3) = reply.processing("Просто текст")
        assertEquals("Начальный экран", msg3)
        assertFalse(isBtn3)
    }

    @Test
    @DisplayName("ReplyClass: навигация Назад")
    fun testReplyNavigationBack() {
        currState = States.CollectionsMenu

        val reply = ReplyClass(
            keyboard = mutableListOf(mutableListOf(Button("Ок"))),
            globalCommand = "/go",
            state = States.CollectionsMenu,
            stateBack = States.GeneralMenu,
            startFunc = { "..." }
        )

        reply.processing("⬅️ Обратно")
        assertEquals(States.GeneralMenu, currState)
    }
}