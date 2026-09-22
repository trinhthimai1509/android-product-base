package dev.sautao.productbase.core.review.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sautao.productbase.core.review.AppReviewManager
import dev.sautao.productbase.core.review.internal.PlayReviewManager
import javax.inject.Singleton

/**
 * Including this module is the whole configuration. There is nothing to tune: Play owns the
 * quota, and the product owns the moment.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ReviewModule {
    @Binds
    @Singleton
    abstract fun bindAppReviewManager(impl: PlayReviewManager): AppReviewManager
}
