package data

class TestRepository {
    fun createTest(title: String, authorId: Long?): Long {
        return Db.insertId("insert into tests(title, author_id) values (?, ?)") { stmt ->
            stmt.setString(1, title)
            if (authorId == null) stmt.setObject(2, null) else stmt.setLong(2, authorId)
        }
    }

    fun addQuestion(testId: Long, position: Int, kind: QuestionKind, prompt: String, options: List<String>, answer: String) {
        Db.execute(
            "insert into test_questions(test_id, position, kind, prompt, options_text, answer_text) values (?, ?, ?, ?, ?, ?)"
        ) { stmt ->
            stmt.setLong(1, testId)
            stmt.setInt(2, position)
            stmt.setString(3, kind.name)
            stmt.setString(4, prompt)
            stmt.setString(5, options.joinToString("|||"))
            stmt.setString(6, answer)
        }
    }

    fun listTests(): List<TestSummary> {
        return Db.query(
            "select id, title, author_id from tests order by created_at desc",
            map = { rs ->
                TestSummary(rs.getLong("id"), rs.getString("title"), rs.getObject("author_id") as? Long)
            }
        )
    }

    fun getTest(testId: Long): TestSummary? {
        return Db.single(
            "select id, title, author_id from tests where id = ?",
            bind = { stmt -> stmt.setLong(1, testId) },
            map = { rs -> TestSummary(rs.getLong("id"), rs.getString("title"), rs.getObject("author_id") as? Long) }
        )
    }

    fun questions(testId: Long): List<TestQuestion> {
        return Db.query(
            "select id, test_id, position, kind, prompt, options_text, answer_text from test_questions where test_id = ? order by position asc, id asc",
            bind = { stmt -> stmt.setLong(1, testId) },
            map = { rs ->
                TestQuestion(
                    id = rs.getLong("id"),
                    testId = rs.getLong("test_id"),
                    position = rs.getInt("position"),
                    kind = QuestionKind.valueOf(rs.getString("kind")),
                    prompt = rs.getString("prompt"),
                    options = rs.getString("options_text")?.split("|||")?.filter { it.isNotBlank() } ?: emptyList(),
                    answer = rs.getString("answer_text") ?: ""
                )
            }
        )
    }

    fun randomTest(): TestSummary? {
        return Db.single(
            "select id, title, author_id from tests order by random() limit 1",
            map = { rs -> TestSummary(rs.getLong("id"), rs.getString("title"), rs.getObject("author_id") as? Long) }
        )
    }

    fun createRun(userId: Long, testId: Long): Long {
        return Db.insertId("insert into test_runs(user_id, test_id) values (?, ?)") { stmt ->
            stmt.setLong(1, userId)
            stmt.setLong(2, testId)
        }
    }

    fun getRun(runId: Long): Triple<Long, Long, Int>? {
        return Db.single(
            "select user_id, test_id, current_index, correct, finished from test_runs where id = ?",
            bind = { stmt -> stmt.setLong(1, runId) },
            map = { rs -> Triple(rs.getLong("user_id"), rs.getLong("test_id"), rs.getInt("current_index")) }
        )
    }

    fun updateRun(runId: Long, currentIndex: Int, correct: Int, finished: Boolean = false) {
        Db.execute(
            """
            update test_runs
            set current_index = ?, correct = ?, finished = ?, finished_at = case when ? then now() else finished_at end
            where id = ?
            """
        ) { stmt ->
            stmt.setInt(1, currentIndex)
            stmt.setInt(2, correct)
            stmt.setBoolean(3, finished)
            stmt.setBoolean(4, finished)
            stmt.setLong(5, runId)
        }
    }

    fun saveAnswer(runId: Long, questionId: Long, answer: String, correct: Boolean) {
        Db.execute(
            "insert into test_answers(run_id, question_id, answer_text, correct) values (?, ?, ?, ?)"
        ) { stmt ->
            stmt.setLong(1, runId)
            stmt.setLong(2, questionId)
            stmt.setString(3, answer)
            stmt.setBoolean(4, correct)
        }
    }

    fun answersForRun(runId: Long): Int {
        return Db.single(
            "select count(*) as c from test_answers where run_id = ?",
            bind = { stmt -> stmt.setLong(1, runId) },
            map = { rs -> rs.getInt("c") }
        ) ?: 0
    }

    fun total(): Int = Db.single("select count(*) as c from tests", map = { it.getInt("c") }) ?: 0
}
