package com.geekloper.borrownote

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

val Context.dbLivres: DBLivres
    get() = DBLivres.getInstance(applicationContext)

class DBLivres(ctx: Context) : ManagedSQLiteOpenHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "livres.db"
        const val DATABASE_VERSION = 11

        const val TABLE_LIVRES = "livres"
        const val TABLE_LIVRES_ID = "id"
        const val COLUMN_LIVRES_TITRE = "titre"
        const val COLUMN_LIVRES_NOTE = "note"
        const val COLUMN_LIVRES_DATE = "date"
        const val COLUMN_LIVRES_IMAGE = "image" // nom du fichier

        private var instance: DBLivres? = null

        @Synchronized
        fun getInstance(ctx: Context): DBLivres {
            if (instance == null) {
                instance = DBLivres(ctx)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(TABLE_LIVRES, true,
                TABLE_LIVRES_ID to INTEGER + PRIMARY_KEY,
                COLUMN_LIVRES_TITRE to TEXT + NOT_NULL,
                COLUMN_LIVRES_NOTE to TEXT + NOT_NULL,
                COLUMN_LIVRES_DATE to INTEGER + NOT_NULL,
                COLUMN_LIVRES_IMAGE to TEXT) // l'image peut être null si pas télécharger
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if(oldVersion < 11) {
            // Mise à jour de la v10 à la v11
            db.execSQL("ALTER TABLE $TABLE_LIVRES ADD $COLUMN_LIVRES_IMAGE TEXT;")
        }
    }
}
