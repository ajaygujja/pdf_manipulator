package com.deepanshuchaudhary.pdf_manipulator

import android.app.Activity
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

// For converting images to pdf.
suspend fun getPdfsFromImages(
    sourceImagesPaths: List<String>,
    createSinglePdf: Boolean,
    context: Activity,
    resultCallback: MethodChannel.Result?,
): List<String> {

    val result = mutableListOf<String>()

    withContext(Dispatchers.IO) {

        val utils = Utils()

        val begin = System.nanoTime()

        val imagesUris = mutableListOf<String>()

        sourceImagesPaths.forEach { imagesPath ->

            val sourceFileUri = utils.getURI(imagesPath)

            val destinationFileName = utils.getFileNameFromPickedDocumentUri(sourceFileUri, context)

            val cachedFilePath: String? = utils.copyFileToCacheDirOnBackground(
                context = context,
                sourceFileUri = sourceFileUri,
                destinationFileName = destinationFileName!!,
                resultCallback = resultCallback,
            )

            imagesUris.add(cachedFilePath!!)

        }

        if (createSinglePdf) {

            val pdfWriterFile: File = File.createTempFile("writerTempFile", ".pdf")
            val pdfWriter = PdfWriter(pdfWriterFile)
            pdfWriter.setSmartMode(true)
            pdfWriter.compressionLevel = 9
            var image = Image(ImageDataFactory.create(imagesUris[0]))
            val pdfDocument = PdfDocument(pdfWriter)
            val doc = Document(pdfDocument, PageSize(image.imageWidth, image.imageHeight))
            for (i in imagesUris.indices) {
                yield()
                image = Image(ImageDataFactory.create(imagesUris[i]))
                pdfDocument.addNewPage(PageSize(image.imageWidth, image.imageHeight))
                image.setFixedPosition(i + 1, 0f, 0f)
                doc.add(image)
                pdfWriter.flush()
            }
            doc.close()
            pdfDocument.close()
            pdfWriter.close()

            result.add(pdfWriterFile.path)

        } else {
            for (i in imagesUris.indices) {
                yield()
                val pdfWriterFile: File = File.createTempFile("writerTempFile", ".pdf")
                val pdfWriter = PdfWriter(pdfWriterFile)
                pdfWriter.setSmartMode(true)
                pdfWriter.compressionLevel = 9
                var image = Image(ImageDataFactory.create(imagesUris[0]))
                val pdfDocument = PdfDocument(pdfWriter)
                val doc = Document(pdfDocument, PageSize(image.imageWidth, image.imageHeight))
                image = Image(ImageDataFactory.create(imagesUris[i]))
                pdfDocument.addNewPage(PageSize(image.imageWidth, image.imageHeight))
                image.setFixedPosition(1, 0f, 0f)
                doc.add(image)
                pdfWriter.flush()
                doc.close()
                pdfDocument.close()
                pdfWriter.close()

                result.add(pdfWriterFile.path)
            }
        }

        val end = System.nanoTime()
        println("Elapsed time in nanoseconds: ${end - begin}")
    }

    return result
}