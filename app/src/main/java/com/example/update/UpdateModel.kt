package com.example.update

data class UpdateInfo(
    val isAvailable: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkName: String
)
