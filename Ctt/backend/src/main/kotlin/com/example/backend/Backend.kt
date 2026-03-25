package com.example.backend

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 10042
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        routing {
            // Simple health check
            get("/health") {
                call.respondText("{\"status\":\"ok\"}", ContentType.Application.Json)
            }

            // Serve the latest APK (copied by the Gradle task)
            get("/download/ctt.apk") {
                val apkFile = File("src/main/resources/static/ctt.apk")
                if (!apkFile.exists()) {
                    call.respond(HttpStatusCode.NotFound, "APK not found – ensure the Android build has run.")
                    return@get
                }
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        "ctt.apk"
                    ).toString()
                )
                call.respondFile(apkFile)
            }

            // Provide song data as JSON for the Android app
            get("/api/songs") {
                val songs = sampleSongs()
                val json = Json.encodeToString(songs)
                call.respondText(json, ContentType.Application.Json)
            }

            // Compatibility endpoint for /songs/latest
            get("/songs/latest") {
                val songs = sampleSongs()
                val json = Json.encodeToString(songs)
                call.respondText(json, ContentType.Application.Json)
            }

            // Endpoint to trigger manual fetch
            post("/fetch-songs") {
                // In a real app, this would trigger a scraper
                call.respondText("{\"status\":\"fetch triggered\"}", ContentType.Application.Json)
            }

            // Thumbnail proxy endpoint
            get("/api/thumbnail") {
                val imageUrl = call.parameters["url"]
                if (imageUrl == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing url parameter")
                    return@get
                }
                // For now, just redirect to the actual image URL
                // In a real proxy, we would fetch and serve the bytes
                call.respondRedirect(imageUrl)
            }
        }
    }.start(wait = true)
}

/** Sample static list of songs – replace with real data source later */
fun sampleSongs(): List<Song> = listOf(
    Song(
        rank = 1,
        title = "Sunrise Beats",
        artist = "DJ Aurora",
        thumbnailUrl = "https://picsum.photos/seed/song1/200/200",
        trend = "rising",
        country = "United States",
        videoUrl = "https://www.tiktok.com/t/ZTY67u9rR/"
    ),
    Song(
        rank = 2,
        title = "Midnight Groove",
        artist = "Nightwave",
        thumbnailUrl = "https://picsum.photos/seed/song2/200/200",
        trend = "steady",
        country = "United Kingdom",
        videoUrl = "https://www.tiktok.com/t/ZTY67u9rR/"
    ),
    Song(
        rank = 3,
        title = "Ocean Whisper",
        artist = "Wave Rider",
        thumbnailUrl = "https://picsum.photos/seed/song3/200/200",
        trend = "falling",
        country = "Canada",
        videoUrl = "https://www.tiktok.com/t/ZTY67u9rR/"
    )
)
