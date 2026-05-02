package com.callvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callvault.app.data.local.entity.DocFeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocFeedbackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: DocFeedbackEntity): Long

    @Query("SELECT * FROM doc_feedback WHERE articleId = :articleId ORDER BY submittedAt DESC")
    fun observeForArticle(articleId: String): Flow<List<DocFeedbackEntity>>

    @Query("""
        SELECT
            SUM(CASE WHEN isHelpful = 1 THEN 1 ELSE 0 END) AS helpful,
            SUM(CASE WHEN isHelpful = 0 THEN 1 ELSE 0 END) AS notHelpful
        FROM doc_feedback WHERE articleId = :articleId
    """)
    suspend fun summary(articleId: String): DocFeedbackSummary?
}

data class DocFeedbackSummary(val helpful: Int, val notHelpful: Int)
