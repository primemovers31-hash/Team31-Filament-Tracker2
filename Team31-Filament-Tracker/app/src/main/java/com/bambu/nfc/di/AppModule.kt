package com.bambu.nfc.di

import android.content.Context
import androidx.room.Room
import com.bambu.nfc.data.auth.GoogleSignInHelper
import com.bambu.nfc.data.firestore.FirestoreSync
import com.bambu.nfc.data.local.AppDatabase
import com.bambu.nfc.data.local.dao.SpoolDao
import com.bambu.nfc.data.repository.SpoolRepositoryImpl
import com.bambu.nfc.domain.repository.SpoolRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bambu_nfc.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSpoolDao(db: AppDatabase): SpoolDao = db.spoolDao()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideGoogleSignInHelper(auth: FirebaseAuth): GoogleSignInHelper {
        // Web client ID from google-services.json (OAuth client type 3)
        val webClientId = "11149312559-ov6k1aq55upj67033k45gpj85s1rti0b.apps.googleusercontent.com"
        return GoogleSignInHelper(auth, webClientId)
    }

    @Provides
    @Singleton
    fun provideSpoolRepository(
        dao: SpoolDao,
        firestoreSync: FirestoreSync
    ): SpoolRepository = SpoolRepositoryImpl(dao, firestoreSync)
}
