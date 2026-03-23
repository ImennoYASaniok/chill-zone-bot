package core

import data.*

object Entry {
    fun buildRouter(): Router {
        return Router(
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
