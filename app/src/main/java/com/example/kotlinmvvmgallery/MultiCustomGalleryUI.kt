package com.example.kotlinmvvmgallery

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinmvvmgallery.adapter.GalleryPicturesAdapter
import com.example.kotlinmvvmgallery.adapter.SpaceItemDecoration
import com.example.kotlinmvvmgallery.model.GalleryPicture
import com.example.kotlinmvvmgallery.viewmodel.GalleryViewModel
import com.jakewharton.processphoenix.ProcessPhoenix
import com.jsibbold.zoomage.ZoomageView
import kotlinx.android.synthetic.main.activity_multi_gallery_ui.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode


class MultiCustomGalleryUI : AppCompatActivity() {



    private val adapter by lazy {
        GalleryPicturesAdapter(pictures, 10)
    }

    private val galleryViewModel: GalleryViewModel by viewModels()

    private val pictures by lazy {
        ArrayList<GalleryPicture>(galleryViewModel.getGallerySize(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_gallery_ui)

        requestReadStoragePermission()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission()
        }
    }


    private var shouldRestart = false

    override fun onResume() {
        super.onResume()
        if (shouldRestart) {
            shouldRestart = false
            ProcessPhoenix.triggerRebirth(this);
        }

    }

    override fun onPause() {
        super.onPause()
        shouldRestart = true
    }


    private fun updateImageSizeForPortrait() {
        val layoutManager = rv.layoutManager as LinearLayoutManager
        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i)
            val imageView = child?.findViewById<ImageView>(R.id.ivImg)
            val layoutParams = imageView?.layoutParams
            if (layoutParams != null) {
                layoutParams.width = 450
            }
            if (layoutParams != null) {
                layoutParams.height = 450
            }
            if (imageView != null) {
                imageView.layoutParams = layoutParams
            }
        }
    }

    private fun requestReadStoragePermission() {
        val readStorage = Manifest.permission.READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(
                this,
                readStorage
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(readStorage), 3)
        } else init()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, 2296)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, 2296)
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(
                this@MultiCustomGalleryUI,
                arrayOf(WRITE_EXTERNAL_STORAGE),
                10
            )
        }
    }

    @SuppressLint("RestrictedApi", "SuspiciousIndentation")
    private fun init() {
        updateToolbar(0)
        val layoutManager = GridLayoutManager(this, 3)
        val pageSize = 20
        rv.layoutManager = layoutManager
        rv.addItemDecoration(SpaceItemDecoration(8))
        rv.adapter = adapter
        val deleter = findViewById<ImageView>(R.id.ivDelete)
        val info = findViewById<ImageView>(R.id.ivInfo)
        val share = findViewById<ImageView>(R.id.ivShare)
        var iPath = Uri.parse("")
        adapter.setOnClickListener { galleryPicture ->
            //showToast(galleryPicture.path)
            findViewById<ZoomageView>(R.id.zoomImg).setImageURI(galleryPicture.path.toUri())
            iPath = Uri.parse(galleryPicture.path)
            zoomImg.visibility = View.VISIBLE
            rv.visibility = View.INVISIBLE
            val recyclerView = findViewById<RecyclerView>(R.id.rv)
            recyclerView.isNestedScrollingEnabled = false
              ivInfo.visibility = View.VISIBLE
              ivShare.visibility = View.VISIBLE

        }

        adapter.setAfterSelectionListener {
            updateToolbar(getSelectedItemsCount())
        }

        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (layoutManager.findLastVisibleItemPosition() == pictures.lastIndex) {
                    loadPictures(pageSize)
                }
            }
        })


        ivBack.setOnClickListener {
            onBackPressed()
        }

        deleter.setOnClickListener {
            val deleteDialog = AlertDialog.Builder(this)
            deleteDialog.setTitle("Delete")
            deleteDialog.setMessage("Do You want to delete chosen images?")
            deleteDialog.setPositiveButton(android.R.string.yes) { dialog, which ->
                deleteSelectedPictures()
            }
            deleteDialog.setNegativeButton(android.R.string.no) { dialog, which ->
                onBackPressed()
            }
            deleteDialog.show()
        }

        info.setOnClickListener {
            if (tvName.visibility == View.VISIBLE) {
                tvName.visibility = View.INVISIBLE
                tvPath.visibility = View.INVISIBLE
                tvDate.visibility = View.INVISIBLE
                tvSize.visibility = View.INVISIBLE
            }else {
                val zoomed = findViewById<ImageView>(R.id.zoomImg)
                //showToast(iPath.toString())
                val inf: InputStream = contentResolver.openInputStream(iPath)!!;
                val exif = ExifInterface(inf)
                val date = exif.getAttribute(ExifInterface.TAG_DATETIME)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                val nameText = findViewById<TextView>(R.id.tvName)
                val pathText = findViewById<TextView>(R.id.tvPath)
                val dateText = findViewById<TextView>(R.id.tvDate)
                val sizeText = findViewById<TextView>(R.id.tvSize)
                val picName = iPath.toString().substringAfterLast("/")
                tvName.visibility = View.VISIBLE
                tvPath.visibility = View.VISIBLE
                tvDate.visibility = View.VISIBLE
                tvSize.visibility = View.VISIBLE

                val cursor: Cursor? = contentResolver.query(iPath,
                    null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val fileNameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val fileName = cursor.getString(fileNameIndex)
                    val pathName = cursor.getString(pathIndex)
                    val sizeKB = cursor.getInt(sizeIndex) * 0.0009765625
                    val sizeRounded = BigDecimal(sizeKB)
                        .setScale(2, RoundingMode.HALF_EVEN)
                    val sizeStr = sizeRounded.toString()
                    val fullPath = "$pathName/$fileName"
                    cursor.close()

                    nameText.text = "Name:\n $fileName"
                    pathText.text = "Path:\n $fullPath"
                    sizeText.text = "Size:\n $sizeStr kB"
                } else {
                    Log.e("error","Cannot get filename")
                }
                if (date == null) {
                    tvDate.visibility = View.INVISIBLE
                } else
                dateText.text = "Date:\n $date"
            }
    }

        share.setOnClickListener {
            ShareCompat.IntentBuilder.from(this)
                .setStream(iPath)
                .setType("image/jpeg")
                .startChooser()
        }

        loadPictures(pageSize)
    }


    private fun deleteSelectedPictures() {
        for (item in adapter.getSelectedItems()) {
            contentResolver.delete(item.path.toUri(),null,null)
            adapter.deleteRefresh(adapter.getPhotoIndex(item.path,adapter.getPhotos()))
            onBackPressed()
        }

    }


    private fun getSelectedItemsCount() = adapter.getSelectedItems().size

    private fun loadPictures(pageSize: Int) {
        galleryViewModel.getImagesFromGallery(this, pageSize) {
            if (it.isNotEmpty()) {
                pictures.addAll(it)
                adapter.notifyItemRangeInserted(pictures.size, it.size)
            }
            Log.i("GalleryListSize", "${pictures.size}")
        }
    }


    private fun updateToolbar(selectedItems: Int) {
        val data = if (selectedItems == 0) {
            ivDelete.visibility = View.GONE
            getString(R.string.txt_gallery)
        } else {
            ivDelete.visibility = View.VISIBLE
            "$selectedItems/${adapter.getSelectionLimit()}"
        }
        tvTitle.text = data
    }

    override fun onBackPressed() {

        if (adapter.removedSelection()) {
            updateToolbar(0)
        } else {
            zoomImg.visibility = View.INVISIBLE
            rv.visibility = View.VISIBLE
            tvName.visibility = View.INVISIBLE
            tvPath.visibility = View.INVISIBLE
            tvDate.visibility = View.INVISIBLE
            tvSize.visibility = View.INVISIBLE
            ivInfo.visibility = View.INVISIBLE
            ivShare.visibility = View.INVISIBLE
            val recyclerView = findViewById<RecyclerView>(R.id.rv)
            recyclerView.isNestedScrollingEnabled = true
        }
    }


    private fun showToast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            init()
        else {
            showToast("Permission Required")
            super.onBackPressed()
        }
    }



}
