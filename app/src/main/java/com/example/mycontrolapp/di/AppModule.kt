package com.example.mycontrolapp.di
import android.content.Context
import androidx.room.Room
import com.example.mycontrolapp.logic.AppDb
import com.example.mycontrolapp.logic.ListManager
import com.example.mycontrolapp.logic.ListManagerRoom
import com.example.mycontrolapp.utils.DeviceInfo
import com.example.mycontrolapp.utils.RemoteEnabled
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @RemoteEnabled @Singleton
    fun provideRemoteEnabled(): Boolean = !DeviceInfo.isEmulator()

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDb =
        Room.databaseBuilder(ctx, AppDb::class.java, "mycontrolapp.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()


//    @Provides @Singleton
//    fun provideListManager(hybrid: ListManagerHybrid): ListManager = hybrid

    @Provides @Singleton
    fun provideListManager(db: AppDb): ListManager = ListManagerRoom(db)

}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .build()
                )
                .build()

        }
}



