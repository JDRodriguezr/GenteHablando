package com.example.gentehablando.di

import com.example.gentehablando.utils.AdManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdManagerEntryPoint {
    fun getAdManager(): AdManager
}
