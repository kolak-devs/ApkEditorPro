package alirezat775.lib.downloader.core

import alirezat775.lib.downloader.core.database.DownloaderDao
import alirezat775.lib.downloader.core.model.DownloaderData
import alirezat775.lib.downloader.core.model.StatusModel
import alirezat775.lib.downloader.helper.ConnectCheckerHelper
import alirezat775.lib.downloader.helper.ConnectionHelper
import alirezat775.lib.downloader.helper.MimeHelper
import android.content.Context
import android.util.Pair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.isCancelled
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.CoroutineContext

/**
 * Author:  Alireza Tizfahm Fard
 * Date:    21/6/2019
 * Email:   alirezat775@gmail.com
 */

internal data class DownloadTask(
    val url: String,
    val context: WeakReference<Context>,
    val dao: DownloaderDao? = null,
    val downloadDir: String? = null,
    val timeOut: Int = 0,
    val downloadListener: OnDownloadListener? = null,
    val header: Map<String, String>? = null,
    var fileName: String? = null,
    var extension: String? = null
) : CoroutineScope {
    // region field
    internal var resume: Boolean = false
    private var connection: HttpURLConnection? = null
    private var downloadedFile: File? = null
    private var downloadedSize: Int = 0
    private var percent: Int = 0
    private var totalSize: Int = 0
    // endregion

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    fun execute() = launch {
        onPreExecute()
        val result = doInBackground()
        onPostExecute(result)
    }

    private fun onPreExecute() {
        // check resume file
        downloadListener?.onStart()
        if (!resume) {
            dao?.insertNewDownload(DownloaderData(0, url, fileName, StatusModel.NEW, 0, 0, 0))
        } else {
            downloadListener?.onResume()
        }
    }

    private suspend fun doInBackground(): Pair<Boolean, Exception?> = withContext(Dispatchers.IO) {
        var result: Pair<Boolean, Exception?>? = null
        try {
            val mUrl = URL(url)
            // open connection
            connection = mUrl.openConnection() as HttpURLConnection
               connection?.doInput = true
            connection?.readTimeout = timeOut
            connection?.connectTimeout = timeOut
            connection?.instanceFollowRedirects = true
            connection?.requestMethod = ConnectionHelper.GET

                //set header request
                if (header != null) {
                    for ((key, value) in header) {
                        connection?.setRequestProperty(key, value)
                    }
                }

                // check file resume able if true set last size to request header
                if (resume) {
                    val model = dao?.getDownloadByUrl(url)
                    percent = model?.percent!!
                    downloadedSize = model.size
                    totalSize = model.totalSize
                    connection?.allowUserInteraction = true
                    connection?.setRequestProperty("Range", "bytes=" + model.size + "-")
                }

            connection?.connect()

                // get filename and file extension
                detectFileName()

                // check file size
                if (!resume) totalSize = connection?.contentLength!!

                // downloaded file
                downloadedFile = File(downloadDir + File.separator + fileName)

                // check file completed
                downloadedFile?.let { file ->
                    if (file.exists() && file.length() == totalSize.toLong()) {
                        result = Pair(true, null)
                    }
                    // buffer file from input stream in connection
                    val bufferedInputStream = BufferedInputStream(connection?.inputStream)
                    val fileOutputStream =
                        if (downloadedSize == 0) FileOutputStream(file)
                        else FileOutputStream(file, true)

                    val bufferedOutputStream = BufferedOutputStream(fileOutputStream, 1024)

                    val buffer = ByteArray(32 * 1024)
                    var len: Int
                    var previousPercent = -1

                    // update percent, size file downloaded
                    while (bufferedInputStream.read(buffer, 0, 1024)
                            .also { len = it } >= 0 && !isCancelled
                    ) {
                        if (!ConnectCheckerHelper.isInternetAvailable(context.get()!!)) {
                            result = Pair(false, IllegalStateException("Please check your network!"))
                        }
                        bufferedOutputStream.write(buffer, 0, len)
                        downloadedSize = downloadedSize.plus(len)
                        percent = (100.0f * downloadedSize.toFloat() / totalSize.toLong()).toInt()
                        if (previousPercent != percent) {
                            downloadListener?.onProgressUpdate(percent, downloadedSize, totalSize)
                            previousPercent = percent
                            dao?.updateDownload(
                                url,
                                StatusModel.DOWNLOADING,
                                percent,
                                downloadedSize,
                                totalSize
                            )
                        }
                    }

                    // close stream and connection
                    bufferedOutputStream.flush()
                    bufferedOutputStream.close()
                    bufferedInputStream.close()
                    connection?.disconnect()
                    result = Pair(true, null)
                }
        } catch (e: Exception) {
            connection?.disconnect()
            result = Pair(false, e)
        }
        return@withContext result!!
    }

    private fun onPostExecute(result: Pair<Boolean, Exception?>) {
        if (result.first) {
            downloadListener?.onCompleted(downloadedFile)
            dao?.updateDownload(url, StatusModel.SUCCESS, percent, downloadedSize, totalSize)
        } else {
            downloadListener?.onFailure(result.second.toString())
        }
    }

    fun onCancelled() {
        connection?.disconnect()
    }

    internal fun cancel() {
        downloadListener?.onCancel()
        dao?.updateDownload(url, StatusModel.FAIL, percent, downloadedSize, totalSize)
    }

    internal fun pause() {
        dao?.updateDownload(url, StatusModel.PAUSE, percent, downloadedSize, totalSize)
        downloadListener?.onPause()
    }

    private fun detectFileName() {
        val contentType = connection?.getHeaderField("Content-Type").toString()
        if (fileName == null) {
            fileName = url.substringAfterLast("/")
            extension = MimeHelper.guessExtensionFromMimeType(contentType)
        }
    }
}