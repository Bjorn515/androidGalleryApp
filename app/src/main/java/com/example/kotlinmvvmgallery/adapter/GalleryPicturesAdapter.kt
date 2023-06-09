package com.example.kotlinmvvmgallery.adapter

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlinmvvmgallery.MultiCustomGalleryUI
import com.example.kotlinmvvmgallery.R
import com.example.kotlinmvvmgallery.model.GalleryPicture
import kotlinx.android.synthetic.main.multi_gallery_listitem.*
import java.io.File


class GalleryPicturesAdapter(private val list: MutableList<GalleryPicture>) : RecyclerView.Adapter<GVH>() {

    init {
        initSelectedIndexList()
    }

    constructor(list: List<GalleryPicture>, selectionLimit: Int) : this(list as MutableList<GalleryPicture>) {
        setSelectionLimit(selectionLimit)
    }

    private var isClicked = false;
    private lateinit var onClick: (GalleryPicture) -> Unit
    private lateinit var afterSelectionCompleted: () -> Unit
    private var isSelectionEnabled = false
    private lateinit var selectedIndexList: ArrayList<Int> // only limited items are selectable.
    private var selectionLimit = 0


    private fun initSelectedIndexList() {
        selectedIndexList = ArrayList(selectionLimit)
    }

    fun setSelectionLimit(selectionLimit: Int) {
        this.selectionLimit = selectionLimit
        removedSelection()
        initSelectedIndexList()
    }

    fun setOnClickListener(onClick: (GalleryPicture) -> Unit) {
        this.onClick = onClick
        isClicked = true;
    }

    fun setAfterSelectionListener(afterSelectionCompleted: () -> Unit) {
        this.afterSelectionCompleted = afterSelectionCompleted
    }

    private fun checkSelection(position: Int) {
        if (isSelectionEnabled) {
            if (getItem(position).isSelected)
                selectedIndexList.add(position)
            else {
                selectedIndexList.remove(position)
                isSelectionEnabled = selectedIndexList.isNotEmpty()
            }
        }
    }

    fun deletePicture(picture: GalleryPicture) {
        deletePicture(list.indexOf(picture))
    }

    fun deletePicture(position: Int) {
        if (File(getItem(position).path).delete()) {
            list.removeAt(position)
            notifyItemRemoved(position)
        } else {
            Log.e("GalleryPicturesAdapter", "Deletion Failed")
        }
    }

    fun deleteRefresh(position: Int) {
        list.removeAt(position)
        notifyItemRemoved(position)
        selectedIndexList.clear()
        notifyDataSetChanged()
    }

    fun infoPath(position: Int): String {
        return getItem(position).path
    }


    fun getPhotoIndex(photoPath: String, photos: List<GalleryPicture>): Int {
        return photos.indexOfFirst { it.path == photoPath }
    }

    fun getPhotos(): List<GalleryPicture> {
        return list
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): GVH {
        val vh = GVH(LayoutInflater.from(p0.context)
            .inflate(R.layout.multi_gallery_listitem, p0, false))
        vh.containerView.setOnClickListener {
            val position = vh.adapterPosition
            val picture = getItem(position)
            if (isSelectionEnabled) {
                handleSelection(position, it.context)
                notifyItemChanged(position)
                checkSelection(position)
                afterSelectionCompleted()

            } else
                onClick(picture)

        }
        vh.containerView.setOnLongClickListener {
            val position = vh.adapterPosition
            isSelectionEnabled = true
            handleSelection(position, it.context)
            notifyItemChanged(position)
            checkSelection(position)
            afterSelectionCompleted()

            isSelectionEnabled
        }
        return vh
    }

    private fun handleSelection(position: Int, context: Context) {

        val picture = getItem(position)

        picture.isSelected = if (picture.isSelected) {
            false
        } else {
            val selectionCriteriaSuccess = getSelectedItems().size < selectionLimit
            if (!selectionCriteriaSuccess)
                selectionLimitReached(context)

            selectionCriteriaSuccess
        }
    }

    fun getSelectionLimit() = selectionLimit

    private fun selectionLimitReached(context: Context) {
        Toast.makeText(
            context,
            "${getSelectedItems().size}/$selectionLimit selection limit reached.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getItem(position: Int) = list[position]

    override fun onBindViewHolder(p0: GVH, p1: Int) {
        val picture = list[p1]
        Glide.with(p0.containerView).load(picture.path).into(p0.ivImg)
        if (picture.isSelected) {
            p0.vSelected.visibility = View.VISIBLE
        } else {
            p0.vSelected.visibility = View.GONE
        }

    }

    override fun getItemCount() = list.size


    fun getSelectedItems() = selectedIndexList.map {
        list[it]
    }


    fun removedSelection(): Boolean {
        return if (isSelectionEnabled) {
            selectedIndexList.forEach {
                list[it].isSelected = false
            }
            isSelectionEnabled = false
            selectedIndexList.clear()
            notifyDataSetChanged()
            true

        } else false
    }

}