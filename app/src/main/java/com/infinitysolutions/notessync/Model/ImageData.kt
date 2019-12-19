package com.infinitysolutions.notessync.Model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images_table")
data class ImageData(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "image_id")var imageId: Long? = null,
    @ColumnInfo(name = "image") var image: String?,
    @ColumnInfo(name = "date_created") var dateCreated: Long,
    @ColumnInfo(name = "g_drive_id") var gDriveId: String?
)