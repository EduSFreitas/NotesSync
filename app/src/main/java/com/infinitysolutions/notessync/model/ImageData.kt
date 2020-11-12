package com.infinitysolutions.notessync.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.infinitysolutions.notessync.contracts.Contract.Companion.TYPE_IMAGE

@Entity(tableName = "images_table")
data class ImageData(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "image_id")var imageId: Long? = null,
    @ColumnInfo(name = "image_path") var imagePath: String,
    @ColumnInfo(name = "date_created") var dateCreated: Long,
    @ColumnInfo(name = "date_modified") var dateModified: Long,
    @ColumnInfo(name = "g_drive_id") var gDriveId: String?,
    @ColumnInfo(name = "type") var type: Int = TYPE_IMAGE
)