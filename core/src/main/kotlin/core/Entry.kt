package core

import data.*
import core.routers.RouterCore

object Entry {
    fun buildRouter(): RouterCore {
        return RouterCore(
            users = UserRepository(),
            memes = MemeRepository(),
            predictions = PredictionRepository(),
            games = GameRepository(),
            tests = TestRepository(),
            events = EventRepository(),
            feedback = FeedbackRepository()
        )
    }
}
