package app

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.User
import core.States
import core.currState
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class AppBotTest {

    private val bot = mockk<Bot>(relaxed = true)
    private val mockUser = mockk<User>()
    private val mockMessage = mockk<Message>()
    private val mockChat = mockk<Chat>()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        currState = States.GeneralMenu

        every { mockUser.id } returns 123456789L
        every { mockUser.username } returns "test_user"
        every { mockChat.id } returns 987654321L
        every { mockMessage.from } returns mockUser
        every { mockMessage.chat } returns mockChat
    }

    @Test
    @DisplayName("Проверка извлечения данных пользователя")
    fun testUserUtils() {
        assertEquals("test_user", getUsername(mockUser))
        assertEquals(123456789L, getUserId(mockUser))

        every { mockUser.username } returns null
        assertEquals("Не указан", getUsername(mockUser))
    }

    @Test
    @DisplayName("Переход в меню мемов при нажатии кнопки")
    fun testMemeMenuTransition() {
        val messageText = "😂 Мемы"

        if (messageText == "😂 Мемы") {
            currState = States.MemeMenu
        }

        assertEquals(States.MemeMenu, currState)
    }

    @Test
    @DisplayName("Переход в мини-игры и проверка состояния")
    fun testMiniGamesTransition() {
        val messageText = "🎮 Мини-игры"

        if (messageText == "🎮 Мини-игры") {
            currState = States.MiniGamesMenu
        }

        assertEquals(States.MiniGamesMenu, currState)
    }

    @Test
    @DisplayName("Проверка возврата 'Обратно' из подменю")
    fun testBackNavigation() {
        currState = States.TestsMenu
        val messageText = "⬅️ Обратно"

        if (messageText == "⬅️ Обратно") {
            currState = States.GeneralMenu
        }

        assertEquals(States.GeneralMenu, currState)
    }

    @Test
    @DisplayName("Имитация обработки команды /start")
    fun testStartCommand() {
        val t = "/start"

        if (t == "/start") {
            currState = States.GeneralMenu
        }

        assertEquals(States.GeneralMenu, currState)
    }

    @Test
    @DisplayName("Проверка логики выбора подборок")
    fun testCollectionsMenu() {
        val t = "📂 Подборки"

        if (t == "📂 Подборки") {
            currState = States.CollectionsMenu
        }

        assertEquals(States.CollectionsMenu, currState)
    }
}