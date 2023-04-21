@file:Suppress("DEPRECATION")

package com.rouxinpai.htmltextview

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import java.lang.ref.WeakReference
import java.net.URI
import kotlin.properties.Delegates

/**
 * author : Saxxhw
 * email  : xingwangwang@cloudinnov.com
 * time   : 2023/4/21 15:52
 * desc   :
 */
class GlideImageGetter(
    textView: TextView,
    baseUrl: String? = null,
    placeHolder: Int = 0,
    matchParentWidth: Boolean = false
) : Html.ImageGetter {

    private val mContainer: TextView
    private var mBaseUri: URI? = null
    private val mPlaceHolder: Int
    private val mMatchParentWidth: Boolean

    private var mCompressImage = false
    private var mQualityImage = 50

    init {
        mContainer = textView
        if (baseUrl != null) {
            mBaseUri = URI.create(baseUrl)
        }
        mPlaceHolder = placeHolder
        mMatchParentWidth = matchParentWidth
    }

    fun enableCompressImage(enable: Boolean) {
        enableCompressImage(enable, 50)
    }

    fun enableCompressImage(enable: Boolean, quality: Int) {
        mCompressImage = enable
        mQualityImage = quality
    }

    override fun getDrawable(source: String?): Drawable {
        val urlDrawable = UrlDrawable()
        if (mPlaceHolder != 0) {
            val placeDrawable = mContainer.context.resources.getDrawable(mPlaceHolder)
            placeDrawable.setBounds(
                0,
                0,
                placeDrawable.intrinsicWidth,
                placeDrawable.intrinsicHeight
            )
            urlDrawable.setBounds(0, 0, placeDrawable.intrinsicWidth, placeDrawable.intrinsicHeight)
            urlDrawable.drawable = placeDrawable
        }
        // get the actual source
        val asyncTask = ImageGetterAsyncTask(
            urlDrawable,
            this,
            mContainer,
            mMatchParentWidth,
            mCompressImage,
            mQualityImage
        )
        asyncTask.execute(source)
        //
        return urlDrawable
    }

    private class ImageGetterAsyncTask(
        d: UrlDrawable,
        imageGetter: GlideImageGetter,
        container: View,
        matchParentWidth: Boolean,
        compressImage: Boolean,
        qualityImage: Int
    ) : AsyncTask<String, Void, Drawable>() {

        private val mDrawableReference: WeakReference<UrlDrawable>
        private val mImageGetter: GlideImageGetter
        private val mContainerReference: WeakReference<View>
        private val mResources: WeakReference<Resources>

        private var mMatchParentWidth: Boolean by Delegates.notNull()
        private var mCompressImage: Boolean by Delegates.notNull()
        private var mQualityImage: Int by Delegates.notNull()

        private lateinit var mSource: String
        private var mScale: Float by Delegates.notNull()

        init {
            mDrawableReference = WeakReference(d)
            mImageGetter = imageGetter
            mContainerReference = WeakReference(container)
            mResources = WeakReference(container.resources)
            mMatchParentWidth = matchParentWidth
            mCompressImage = compressImage
            mQualityImage = qualityImage
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: String): Drawable? {
            mSource = params.first()
            val resources = mResources.get()
            if (resources != null) {
                return if (mCompressImage) {
                    fetchCompressedDrawable(resources, mSource)
                } else {
                    fetchDrawable(resources, mSource)
                }
            }
            return null
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: Drawable?) {
            super.onPostExecute(result)
            if (result == null) {
                Log.d(this::class.simpleName, "Drawable result is null! (source: $mSource)")
                return
            }
            val urlDrawable = mDrawableReference.get() ?: return
            // set the correct bound according to the result from HTTP call
            urlDrawable.setBounds(
                0,
                0,
                (result.intrinsicWidth * mScale).toInt(),
                (result.intrinsicHeight * mScale).toInt()
            )

            // change the reference of the current drawable to the result from the HTTP call
            urlDrawable.drawable = result

            // redraw the image by invalidating the container
            mImageGetter.mContainer.invalidate()
            // redraw the image by invalidating the container
            mImageGetter.mContainer.text = mImageGetter.mContainer.text
        }

        /**
         * Get the Drawable from URL
         */
        private fun fetchDrawable(
            @Suppress("UNUSED_PARAMETER") res: Resources,
            urlString: String
        ): Drawable? {
            try {
                val context = mContainerReference.get()?.context ?: return null
                val futureTarget = Glide.with(context).asDrawable().load(urlString).submit()
                val drawable = futureTarget.get()

                mScale = getScale(drawable)
                drawable.setBounds(
                    0,
                    0,
                    (drawable.intrinsicWidth * mScale).toInt(),
                    (drawable.intrinsicHeight * mScale).toInt()
                )
                return drawable
            } catch (e: Exception) {
                return null
            }
        }

        /**
         * Get the compressed image with specific quality from URL
         */
        private fun fetchCompressedDrawable(res: Resources, urlString: String): Drawable? {
            try {
                val context = mContainerReference.get()?.context ?: return null
                val futureTarget = Glide.with(context).asBitmap().load(urlString).submit()
                val bitmap = futureTarget.get()

                mScale = getScale(bitmap)
                val b = BitmapDrawable(res, bitmap)

                b.setBounds(
                    0,
                    0,
                    (b.intrinsicWidth * mScale).toInt(),
                    (b.intrinsicHeight * mScale).toInt()
                )
                return b
            } catch (e: Exception) {
                return null
            }
        }

        private fun getScale(bitmap: Bitmap): Float {
            val container = mContainerReference.get() ?: return 1f
            val maxWidth: Float = container.width.toFloat()
            val originalDrawableWidth: Float = bitmap.width.toFloat()
            return maxWidth / originalDrawableWidth
        }

        private fun getScale(drawable: Drawable): Float {
            val container = mContainerReference.get()
            if (!mMatchParentWidth || container == null) {
                return 1f
            }
            val maxWidth: Float = container.width.toFloat()
            val originalDrawableWidth: Float = drawable.intrinsicWidth.toFloat()
            return maxWidth / originalDrawableWidth
        }
    }

    /**
     *
     */
    @Suppress("DEPRECATION")
    class UrlDrawable : BitmapDrawable() {

        var drawable: Drawable? = null

        override fun draw(canvas: Canvas) {
            super.draw(canvas)
            drawable?.draw(canvas)
        }
    }
}