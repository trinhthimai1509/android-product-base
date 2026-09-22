package dev.sautao.productbase.core.database

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Stores [Instant] as epoch milliseconds.
 *
 * Milliseconds rather than an ISO string because it sorts and range-queries correctly in SQL
 * without a function call, and because it is timezone-free by construction — a stored instant
 * cannot silently acquire the writer's timezone.
 *
 * A column holding a *calendar date* (a birthday, a due date) is a different problem and should
 * not use this: convert at the product's boundary with an explicit zone. Converters for those
 * types belong here too, once a product actually needs one.
 */
class InstantConverters {
    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)
}
